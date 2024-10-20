Machine: Oracle Cloud - VM.Standard.A1.Flex (1 ARM core, 6 GB RAM)

Command: `hyperfine -r 100 --export-markdown bench.md "java -jar signum-grpc-test-all.jar h" "java -jar signum-grpc-test-all.jar g"`

| Command                                |      Mean [s] | Min [s] | Max [s] |    Relative |
| :------------------------------------- | ------------: | ------: | ------: | ----------: |
| `java -jar signum-grpc-test-all.jar h` | 3.958 ± 0.370 |   3.630 |   5.141 | 2.33 ± 0.24 |
| `java -jar signum-grpc-test-all.jar g` | 1.699 ± 0.081 |   1.565 |   1.870 |        1.00 |
