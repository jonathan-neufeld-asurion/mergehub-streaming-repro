
ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val ScalapbCirceVersion        = "0.7.1"
lazy val CatsVersion                = "2.0.0"
lazy val AkkaVersion                = "2.6.19"
lazy val AkkaHttpVersion            = "10.2.7"
lazy val GoogleCommonProtoVersion   = "1.18.0-0"
lazy val ProtobufJavaVersion        = "3.19.2"

lazy val subModuleSettings = Seq(
  ideExcludedDirectories := baseDirectory.value / "target" :: Nil
)

lazy val model = (project in file("model"))
  .enablePlugins(AkkaGrpcPlugin)
  .settings(
    subModuleSettings,
    ideExcludedDirectories ++= baseDirectory.value / "target" / "streams" :: baseDirectory.value / "target" / "protobuf_external" :: Nil,
    akkaGrpcCodeGeneratorSettings += "server_power_apis",
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.10" % GoogleCommonProtoVersion % "protobuf",
      "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.10" % GoogleCommonProtoVersion,
      "com.google.protobuf"                 % "protobuf-java"                           % ProtobufJavaVersion
    )
  )

lazy val emitterTest = (project in file("emitter"))
  .dependsOn(model)
  .settings(
    subModuleSettings,
    libraryDependencies ++= Seq(
      "io.github.scalapb-json" %% "scalapb-circe"      % ScalapbCirceVersion,
      "org.typelevel"          %% "cats-core"          % CatsVersion,
      "org.typelevel"          %% "cats-effect"        % CatsVersion,
      "com.typesafe.akka"      %% "akka-http2-support" % AkkaHttpVersion,
      "com.typesafe.akka"      %% "akka-discovery"     % AkkaVersion,
      "com.typesafe.akka"      %% "akka-stream"        % AkkaVersion
    )
  )

lazy val sinkTest = (project in file("sink"))
  .dependsOn(model)
  .settings(
    subModuleSettings,
    libraryDependencies ++= Seq(
      "io.github.scalapb-json" %% "scalapb-circe"      % ScalapbCirceVersion,
      "org.typelevel"          %% "cats-effect"        % CatsVersion,
      "com.typesafe.akka"      %% "akka-http2-support" % AkkaHttpVersion,
      "com.typesafe.akka"      %% "akka-stream"        % AkkaVersion,
      "com.typesafe.akka"      %% "akka-discovery"     % AkkaVersion,
      "com.typesafe.akka"      %% "akka-stream"        % AkkaVersion,
      "com.typesafe.akka"      %% "akka-http-core"     % AkkaHttpVersion,
      "com.typesafe.akka"      %% "akka-parsing"       % AkkaHttpVersion,
      "com.typesafe.akka"      %% "akka-http"          % AkkaHttpVersion
    )
  )

lazy val root = (project in file("."))
  .aggregate(
    model,
    emitterTest,
    sinkTest
  )

