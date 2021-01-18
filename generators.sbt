val generateTupleCombinatorsFrom = 2
val generateTupleCombinatorsTo = 9

Compile / sourceGenerators += Def.task {
  Seq.concat(
    GenerateCombineEventStreams(
      (Compile / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run,
    GenerateCombineSignals(
      (Compile / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run,
    GenerateSampleCombineEventStreams(
      (Compile / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run,
    GenerateSampleCombineSignals(
      (Compile / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run,
    GenerateTupleEventStreams(
      (Compile / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run,
    GenerateTupleSignals(
      (Compile / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run,
    GenerateCombinableEventStream(
      (Compile / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run,
    GenerateCombinableSignal(
      (Compile / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run,
    GenerateStaticEventStreamCombineOps(
      (Compile / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run,
    GenerateStaticSignalCombineOps(
      (Compile / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run
  )
}.taskValue

Test / sourceGenerators += Def.task {
  Seq.concat(
    GenerateCombineSignalsTest(
      (Test / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run,
    GenerateCombineEventStreamsTest(
      (Test / sourceDirectory).value,
      from = generateTupleCombinatorsFrom,
      to = generateTupleCombinatorsTo
    ).run
  )
}.taskValue
