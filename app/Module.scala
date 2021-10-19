import java.time.Clock

import Module.{BackgroundJob, CollectorProvider, IndexProvider, RecognitionProvider}
import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{KillSwitches, Materializer}
import com.github.gervaisb.effinlib.collector.{Collector, CollectorComponents}
import com.github.gervaisb.effinlib.index.{Index, IndexComponents}
import com.github.gervaisb.effinlib.recognition.{Recognition, RecognitionComponents}
import com.github.gervaisb.effinlib.{Publication, Words}
import com.google.inject.{AbstractModule, Provider}
import com.redis.RedisClientPool
import javax.inject.Inject
import play.api.inject.ApplicationLifecycle
import play.api.libs.ws.{StandaloneWSClient, StandaloneWSRequest, WSClient}
import play.api.{Configuration, Environment, Logger}

import scala.concurrent.{ExecutionContext, Future}

/**
 * This class is a Guice module that tells Guice how to bind several
 * different types. This Guice module is created when the Play
 * application starts.

 * Play will automatically use any class called `Module` that is in
 * the root package. You can create modules in other locations by
 * adding `play.modules.enabled` settings to the `application.conf`
 * configuration file.
 */
class Module(val environment: Environment, configuration:Configuration) extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[Collector]).toProvider(classOf[CollectorProvider])
    bind(classOf[Recognition]).toProvider(classOf[RecognitionProvider])
    bind(classOf[Index]).toProvider(classOf[IndexProvider]).asEagerSingleton()

    // Ask Guice to create an instance/start the job when the application starts.
    bind(classOf[BackgroundJob]).asEagerSingleton()
  }

}
object Module {

  class BackgroundJob @Inject()(lifecycle: ApplicationLifecycle, collector: Collector, recognition: Recognition, index: Index, configuration: Configuration, system: ActorSystem) {
    import scala.concurrent.duration._

    private implicit val mat:Materializer = Materializer.matFromSystem(system)
    private implicit val ec:ExecutionContext = system.dispatcher
    private val log = Logger("application.job")

    private val (process, completion) = {
      val publications: Source[Publication, NotUsed] =
        Source.repeat(1).mapConcat(_ => collector.next())

      val recognized: Source[Publication with Words, NotUsed] = publications
        .filterNot(index.contains)
        .throttle(configuration.get[Int]("recognition.daily_limit"), 1.day)
        .mapAsync(1)(recognition.recognize)

      recognized.viaMat(KillSwitches.single)(Keep.right)
        .toMat(Sink.foreach(index.append))(Keep.both).run()

    }
    log.info("Indexation flow started")


    lifecycle.addStopHook { () =>
      process.shutdown()
      completion.map(_ =>{
        log.info("Indexation flow stopped successfully")
      })
    }
  }

  class CollectorProvider @Inject()(configuration: Configuration) extends Provider[Collector] with CollectorComponents {
    override def get(): Collector = collector(configuration.underlying)
  }

  class RecognitionProvider @Inject()(configuration:Configuration, system: ActorSystem, wsClient: WSClient) extends Provider[Recognition] with RecognitionComponents  {
    override def get(): Recognition = recognition(configuration.underlying, system, WsClientAsStandalone(wsClient))
  }

  class IndexProvider @Inject()(configuration: Configuration, lifecycle: ApplicationLifecycle) extends Provider[Index] with IndexComponents {
    override def get(): Index = {
      val redis = new RedisClientPool(
        configuration.get[String]("redis.host"),
        configuration.get[Int]("redis.port")
      )
      lifecycle.addStopHook {
        () => Future.successful(redis.close())
      }
      index(redis)
    }
  }

  case class WsClientAsStandalone(wsClient: WSClient) extends StandaloneWSClient {
    override def underlying[T]: T = wsClient.asInstanceOf[T]

    override def url(url: String): StandaloneWSRequest = wsClient.url(url)

    override def close(): Unit = wsClient.close()
  }
}
