import de.heikoseeberger.sbtheader.License

import scalariform.formatter.preferences._

val scalariformPrefs = scalariformPreferences := scalariformPreferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(DoubleIndentConstructorArguments, true)
  .setPreference(PreserveSpaceBeforeArguments, true)
  .setPreference(RewriteArrowSymbols, true)
  .setPreference(DoubleIndentConstructorArguments, true)
  .setPreference(DanglingCloseParenthesis, Preserve)
  .setPreference(SpaceBeforeContextColon, true)
  .setPreference(NewlineAtEndOfFile, true)

name := "fluence"

version := "0.1"

val scalaV = scalaVersion := "2.12.4"

scalacOptions in Compile ++= Seq("-Ypartial-unification", "-Xdisable-assertions")

javaOptions in Test ++= Seq("-ea")

val commons = Seq(
  scalaV,
  scalariformPrefs,
  fork in test := true,
  parallelExecution in Test := false,
  organizationName := "Fluence Labs Limited",
  organizationHomepage := Some(new URL("https://fluence.ai")),
  startYear := Some(2017),
  licenses += ("AGPL-3.0", new URL("http://www.gnu.org/licenses/agpl-3.0.en.html")),
  headerLicense := Some(License.AGPLv3("2017", organizationName.value))
)

commons

enablePlugins(AutomateHeaderPlugin)

val RocksDbV = "5.8.0"
val TypeSafeConfV = "1.3.2"
val FicusV = "1.4.2"
val MockitoV = "2.11.0"
val MonocleV = "1.5.0-cats-M2"

val logback = "ch.qos.logback" % "logback-classic" % "1.2.+"

val cats1 = "org.typelevel" %% "cats-core" % "1.0.0-RC1"
val monix3 = "io.monix" %% "monix" % "3.0.0-M2"
val shapeless = "com.chuusai" %% "shapeless" % "2.3.+"
val monocle = "com.github.julien-truffaut" %% "monocle-core" % MonocleV
val monocleMacro = "com.github.julien-truffaut" %% "monocle-macro" % MonocleV

val rocksDb = "org.rocksdb" % "rocksdbjni" % RocksDbV
val typeSafeConfig = "com.typesafe" % "config" % TypeSafeConfV
val ficus = "com.iheart" %% "ficus" % FicusV

val mockito = "org.mockito" % "mockito-core" % MockitoV % Test
val scalatest = "org.scalatest" %% "scalatest" % "3.0.+" % Test

val grpc = Seq(
  PB.targets in Compile := Seq(
    scalapb.gen() -> (sourceManaged in Compile).value
  ),
  libraryDependencies ++= Seq(
    "com.trueaccord.scalapb" %% "scalapb-runtime" % com.trueaccord.scalapb.compiler.Version.scalapbVersion % "protobuf",
    "io.grpc" % "grpc-netty" % com.trueaccord.scalapb.compiler.Version.grpcJavaVersion,
    "com.trueaccord.scalapb" %% "scalapb-runtime-grpc" % com.trueaccord.scalapb.compiler.Version.scalapbVersion
  )
)
val chill = "com.twitter" %% "chill" % "0.9.2"

lazy val `fluence` = project.in(file("."))
  .settings(commons)
  .settings(
    libraryDependencies ++= Seq(
      scalatest
    )
  ).aggregate(
  `node`,
  `storage`,
  `b-tree-client`,
  `b-tree-server`,
  `crypto`
).enablePlugins(AutomateHeaderPlugin)

lazy val `codec-core` = project.in(file("codec/core"))
.settings(commons)
.settings(
  libraryDependencies ++= Seq(
    cats1
  )
)

lazy val `codec-kryo` = project.in(file("codec/kryo"))
.settings(commons)
.settings(
  libraryDependencies ++= Seq(
    chill,
    shapeless,
    scalatest
  )
).dependsOn(`codec-core`).aggregate(`codec-core`)

lazy val `kademlia-node` = project.in(file("kademlia/node"))
  .settings(commons)
  .settings(
    libraryDependencies ++= Seq(
      cats1,
      logback,
      scalatest,
      monix3 % Test
    )
  ).dependsOn(`kademlia-protocol`).aggregate(`kademlia-protocol`)

lazy val `kademlia-protocol` = project.in(file("kademlia/protocol"))
  .settings(commons)
  .settings(
    libraryDependencies += cats1
  ).dependsOn(`codec-core`)

lazy val `kademlia-grpc` = project.in(file("kademlia/grpc"))
  .settings(commons)
  .settings(
    grpc
  ).dependsOn(`kademlia-protocol`, `codec-core`).aggregate(`kademlia-protocol`)

lazy val `transport-grpc` = project.in(file("transport/grpc"))
  .settings(commons)
  .settings(
    grpc,
    libraryDependencies ++= Seq(
      monix3,
      shapeless,
      typeSafeConfig,
      ficus,
      logback,
      "org.bitlet" % "weupnp" % "0.1.+",
      scalatest
    )
  ).dependsOn(`transport-core`)

lazy val `transport-core` = project.in(file("transport/core"))
  .settings(commons)
  .settings(
    libraryDependencies ++= Seq(
      cats1,
      shapeless
    )
  ).dependsOn(`kademlia-protocol`).aggregate(`kademlia-protocol`)

// TODO: separate API from implementation for both serialization and rocksDB
lazy val `storage` = project.in(file("storage"))
  .settings(commons)
  .settings(
    libraryDependencies ++= Seq(
      rocksDb,
      typeSafeConfig,
      ficus,
      monix3,
      shapeless,
      scalatest,
      mockito
    )
  ).dependsOn(`codec-core`).aggregate(`codec-core`)

lazy val `b-tree-client` = project.in(file("b-tree/client"))
  .settings(commons)
  .settings(
    libraryDependencies ++= Seq(
      monix3,
      logback,
      scalatest
    )
  ).dependsOn(`b-tree-common`, `b-tree-protocol`)
   .aggregate(`b-tree-common`, `b-tree-protocol`)

lazy val `b-tree-common` = project.in(file("b-tree/common"))
  .settings(commons)
  .settings(
    libraryDependencies ++= Seq(
      cats1,
      scalatest
    )
  ).dependsOn(`crypto`)

lazy val `b-tree-protocol` = project.in(file("b-tree/protocol"))
  .settings(commons)
  .dependsOn(`b-tree-common`)
  .aggregate(`b-tree-common`)

lazy val `b-tree-server` = project.in(file("b-tree/server"))
  .settings(commons)
  .settings(
    libraryDependencies ++= Seq(
      typeSafeConfig,
      ficus,
      monix3,
      logback,
      scalatest
    )
  ).dependsOn(`storage`, `codec-kryo`, `b-tree-common`, `b-tree-protocol`, `b-tree-client` % "compile->test")
   .aggregate(`b-tree-common`, `b-tree-protocol`)

lazy val `crypto` = project.in(file("crypto"))
  .settings(commons)
  .settings(
    libraryDependencies ++= Seq(
      scalatest
    )
  )

lazy val `dataset-node` = project.in(file("dataset/node"))
  .settings(commons)
  .settings(
    libraryDependencies ++= Seq(
      cats1,
      scalatest
    )
  ).dependsOn(`storage`, `kademlia-node`, `dataset-protocol`)

lazy val `dataset-protocol` = project.in(file("dataset/protocol"))
  .settings(commons)
  .dependsOn(`kademlia-protocol`).aggregate(`kademlia-protocol`)

lazy val `dataset-grpc` = project.in(file("dataset/grpc"))
  .settings(commons)
  .settings(
    grpc,
    libraryDependencies ++= Seq(
      cats1
    )
  ).dependsOn(`dataset-protocol`, `codec-core`).aggregate(`dataset-protocol`)

lazy val `node` = project.in(file("node"))
  .settings(commons)
  .settings(
    libraryDependencies ++= Seq(
      scalatest
    )
  )
  .dependsOn(`transport-grpc`, `kademlia-grpc`, `kademlia-node`, `dataset-node`, `dataset-grpc`)
  .aggregate(`transport-grpc`, `kademlia-grpc`, `kademlia-node`, `dataset-node`, `dataset-grpc`)