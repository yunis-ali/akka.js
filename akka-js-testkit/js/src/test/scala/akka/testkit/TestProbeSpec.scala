package akka.testkit

import language.postfixOps

import akka.actor._
import akka.testkit.Await
import scala.concurrent.duration._
import akka.pattern.ask
import scala.util.Try
import java.util.concurrent.atomic.AtomicInteger

class TestProbeSpec extends AkkaSpec with DefaultTimeout {

  "A TestProbe" must {

    "reply to futures" in {
      val tk = TestProbe()
      val future = tk.ref ? "hello"

      await()

      tk.expectMsg(0 millis, "hello") // TestActor runs on CallingThreadDispatcher
      tk.lastMessage.sender ! "world"
      //future should be('completed)
      Await.result(future, timeout.duration) should ===("world")
    }

    "reply to messages" in {
      val tk1 = TestProbe()
      val tk2 = TestProbe()

      await()

      tk1.ref.!("hello")(tk2.ref)

      await()

      tk1.expectMsg(0 millis, "hello")

      await()

      tk1.lastMessage.sender ! "world"

      await()

      tk2.expectMsg(0 millis, "world")
    }

    "properly send and reply to messages" in {
      val probe1 = TestProbe()
      val probe2 = TestProbe()

      await()

      probe1.send(probe2.ref, "hello")

      await()

      probe2.expectMsg(0 millis, "hello")

      await()

      probe2.lastMessage.sender ! "world"

      await()

      probe1.expectMsg(0 millis, "some hint here", "world")
    }

    "create a child when invoking actorOf" in {
      val probe = TestProbe()
      val child = probe.childActorOf(TestActors.echoActorProps)
      child.path.parent should be(probe.ref.path)

      val namedChild = probe.childActorOf(TestActors.echoActorProps, "actorName")
      namedChild.path.name should be("actorName")
    }

    "restart a failing child if the given supervisor says so" in {
      val restarts = new AtomicInteger(0)

      class FailingActor extends Actor {
        override def receive = msg ⇒ msg match {
          case _ ⇒
            throw new RuntimeException("simulated failure")
        }

        override def postRestart(reason: Throwable): Unit = {
          restarts.incrementAndGet()
        }
      }

      val probe = TestProbe()
      val child = probe.childActorOf(Props(new FailingActor), SupervisorStrategy.defaultStrategy)

      awaitAssert {
        child ! "hello"
        restarts.get() should be > (1)
      }
    }

    def assertFailureMessageContains(expectedHint: String)(block: ⇒ Unit) {
      Try {
        block
      } match {
        case scala.util.Failure(e: AssertionError) ⇒
          if (!(e.getMessage contains expectedHint))
            fail(s"failure message did not contain hint! Was: ${e.getMessage}, expected to contain $expectedHint")
        case scala.util.Failure(oth) ⇒
          fail(s"expected AssertionError but got: $oth")
        case scala.util.Success(result) ⇒
          fail(s"expected failure but got: $result")
      }
    }

    "throw AssertionError containing hint in its message if max await time is exceeded" in {
      val probe = TestProbe()
      val hint = "some hint"

      assertFailureMessageContains(hint) {
        probe.expectMsg(0 millis, hint, "hello")
      }
    }

    "throw AssertionError containing hint in its message if received message doesn't match" in {
      val probe = TestProbe()
      val hint = "some hint"

      assertFailureMessageContains(hint) {
        probe.ref ! "hello"
        probe.expectMsg(0 millis, hint, "bye")
      }
    }

    "have an AutoPilot" in {
      //#autopilot
      val probe = TestProbe()
      probe.setAutoPilot(new TestActor.AutoPilot {
        def run(sender: ActorRef, msg: Any): TestActor.AutoPilot =
          msg match {
            case "stop" ⇒ TestActor.NoAutoPilot
            case x      ⇒ testActor.tell(x, sender); TestActor.KeepRunning
          }
      })
      //#autopilot
      probe.ref ! "hallo"
      probe.ref ! "welt"
      probe.ref ! "stop"
      expectMsg("hallo")
      expectMsg("welt")
      probe.expectMsg("hallo")
      probe.expectMsg("welt")
      probe.expectMsg("stop")
      probe.ref ! "hallo"
      probe.expectMsg("hallo")
      testActor ! "end"
      expectMsg("end") // verify that "hallo" did not get through
    }

    "be able to expect primitive types" in {
      for (_ ← 1 to 2) testActor ! 42
      //Adapted because of different semantics in ScalaJS
      expectMsgType[AnyVal] should be(42)
      expectMsgAnyClassOf(classOf[AnyVal]) should be(42)
      /*
      expectMsgAllClassOf(classOf[AnyVal]) should be(Seq(42))
      expectMsgAllConformingOf(classOf[AnyVal]) should be(Seq(42))
      expectMsgAllConformingOf(5 seconds, classOf[AnyVal]) should be(Seq(42))
      expectMsgAllClassOf(classOf[AnyVal]) should be(Seq(42))
      expectMsgAllClassOf(5 seconds, classOf[AnyVal]) should be(Seq(42))
      */
    }

    "be able to ignore primitive types" in {
      ignoreMsg { case 42 ⇒ true }

      await()

      testActor ! 42
      testActor ! "pigdog"

      await()

      expectMsg("pigdog")
    }

    "watch actors when queue non-empty" in {
      val probe = TestProbe()
      // deadLetters does not send Terminated
      val target = system.actorOf(Props(new Actor {
        def receive = Actor.emptyBehavior
      }))
      system.stop(target)
      probe.ref ! "hello"
      probe watch target
      probe.expectMsg(1.seconds, "hello")
      probe.expectMsg(1.seconds, Terminated(target)(false, false))
    }

    "allow user-defined name" in {
      val probe = TestProbe("worker")
      probe.ref.path.name should startWith("worker")
    }

    "have reasonable default name" in {
      val probe = new TestProbe(system)
      probe.ref.path.name should startWith("testProbe")
    }
  }
}
