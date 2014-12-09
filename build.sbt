name := "ru.nkdhny.runtag.filestorage"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.10.3"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8","-Xmax-classfile-name", "255")

libraryDependencies ++= {
  val akkaV = "2.1.4"
  val sprayV = "1.1.1"
  Seq(
    "ru.nkdhny"           %%  "photo-processor-java-support" % "0.1-SNAPSHOT",
    "io.spray"            %   "spray-can"        % sprayV,
    "io.spray"            %%  "spray-json"       % "1.2.6",
    "io.spray"            %   "spray-routing"    % sprayV,
    "io.spray"            %   "spray-testkit"    % sprayV  % "test",
    "com.typesafe.akka"   %%  "akka-actor"       % akkaV,
    "com.typesafe.akka"   %%  "akka-testkit"     % akkaV   % "test",
    "org.specs2"          %%  "specs2"           % "2.2.3" % "test",
    "com.github.nscala-time" %% "nscala-time"    % "1.2.0",
    "org.mockito"          %   "mockito-all"     % "1.9.5",
    "com.github.mauricio" %% "postgresql-async"  % "0.2.+",
    "org.scalikejdbc"     %% "scalikejdbc-async" % "0.5.4-SNAPSHOT"
  )
}
