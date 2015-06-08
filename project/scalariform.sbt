import scalariform.formatter.preferences._

// コードフォーマッタプラグインscalariformの設定をロード
// cf. scalariform.sbt
scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(RewriteArrowSymbols, true)
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignParameters, true)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(SpacesWithinPatternBinders, true)
