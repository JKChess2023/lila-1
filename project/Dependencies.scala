import play.sbt.PlayImport._
import sbt._, Keys._

object Dependencies {

  val lilaMaven = "lila-maven" at "https://raw.githubusercontent.com/Mind-Sports-Games/lila-maven/master"
  val wslLocalMaven = "wsl-local-maven" at "file:////home/jheps/.m2/repository/"
  val dockerLocalMaven = "docker-local-maven" at "file:////home/playstrategy/.m2/repository/"
  //TODO change to use sys.env as in SG and update in buildSettings.scala

  val scalalib      = "com.github.ornicar"         %% "scalalib"                        % "7.0.2"
  val hasher        = "com.roundeights"            %% "hasher"                          % "1.2.1"
  val jodaTime      = "joda-time"                   % "joda-time"                       % "2.10.10"
  val compression   = "org.lichess"                %% "compression"                     % "1.6"
  val strategyGames = "org.playstrategy"           %% "strategygames"                   % "10.2.1-pstrat37_13"
  val maxmind       = "com.sanoma.cda"             %% "maxmind-geoip2-scala"            % "1.3.1-THIB"
  val prismic       = "io.prismic"                 %% "scala-kit"                       % "1.2.19-THIB213"
  val scrimage      = "com.sksamuel.scrimage"       % "scrimage-core"                   % "4.0.18"
  val scaffeine     = "com.github.blemale"         %% "scaffeine"                       % "4.0.2"  % "compile"
  val googleOAuth   = "com.google.auth"             % "google-auth-library-oauth2-http" % "0.25.5"
  val scalaUri      = "io.lemonlabs"               %% "scala-uri"                       % "3.2.0"
  val scalatags     = "com.lihaoyi"                %% "scalatags"                       % "0.9.4"
  val lettuce       = "io.lettuce"                  % "lettuce-core"                    % "6.1.2.RELEASE"
  val epoll         = "io.netty"                    % "netty-transport-native-epoll"    % "4.1.58.Final" classifier "linux-x86_64"
  val autoconfig    = "io.methvin.play"            %% "autoconfig-macros"               % "0.3.2"  % "provided"
  val scalatest     = "org.scalatest"              %% "scalatest"                       % "3.1.0"  % Test
  val uaparser      = "org.uaparser"               %% "uap-scala"                       % "0.13.0"
  val specs2        = "org.specs2"                 %% "specs2-core"                     % "4.11.0" % Test
  val apacheText    = "org.apache.commons"          % "commons-text"                    % "1.9"
  val bloomFilter   = "com.github.alexandrnikitin" %% "bloom-filter"                    % "0.13.1"

  object flexmark {
    val version = "0.50.50"
    val bundle =
      ("com.vladsch.flexmark" % "flexmark" % version) ::
        List("formatter", "ext-tables", "ext-autolink", "ext-gfm-strikethrough").map { ext =>
          "com.vladsch.flexmark" % s"flexmark-$ext" % version
        }
  }

  object macwire {
    val version = "2.3.7"
    val macros  = "com.softwaremill.macwire" %% "macros" % version % "provided"
    val util    = "com.softwaremill.macwire" %% "util"   % version % "provided"
    def bundle  = Seq(macros, util)
  }

  object reactivemongo {
    val version = "1.0.4"

    val driver = "org.reactivemongo" %% "reactivemongo"               % version
    val stream = "org.reactivemongo" %% "reactivemongo-akkastream"    % version
    val epoll  = "org.reactivemongo"  % "reactivemongo-shaded-native" % s"$version-linux-x86-64"
    val kamon  = "org.reactivemongo" %% "reactivemongo-kamon"         % version
    def bundle = Seq(driver, stream)
  }

  object play {
    val version = "2.8.7-lila_1.6"
    val api     = "com.typesafe.play" %% "play"        % version
    val json    = "com.typesafe.play" %% "play-json"   % "2.9.2"
    val mailer  = "com.typesafe.play" %% "play-mailer" % "8.0.1"
  }

  object playWs {
    val version = "2.1.2"
    val ahc     = "com.typesafe.play" %% "play-ahc-ws-standalone"  % version
    val json    = "com.typesafe.play" %% "play-ws-standalone-json" % version
    val bundle  = Seq(ahc, json)
  }

  object kamon {
    val version    = "2.1.18"
    val core       = "io.kamon" %% "kamon-core"           % version
    val influxdb   = "io.kamon" %% "kamon-influxdb"       % version
    val metrics    = "io.kamon" %% "kamon-system-metrics" % version
    val prometheus = "io.kamon" %% "kamon-prometheus"     % version
  }
  object akka {
    val version    = "2.6.8"
    val akka       = "com.typesafe.akka" %% "akka-actor"       % version
    val akkaTyped  = "com.typesafe.akka" %% "akka-actor-typed" % version
    val akkaStream = "com.typesafe.akka" %% "akka-stream"      % version
    val akkaSlf4j  = "com.typesafe.akka" %% "akka-slf4j"       % version
    val testkit    = "com.typesafe.akka" %% "akka-testkit"     % version % Test
    def bundle     = List(akka, akkaTyped, akkaStream, akkaSlf4j)
  }
}
