# Java Keyword Counter

*Степан Остапенко, тестовое задание для проекта **ML-based path selection strategy for symbolic execution***

---

Задание реализовано на kotlin.

Сборка производится с помощью скрипта `build.sh`:
```shell
./build.sh
```

После этого в корне проекта должен появиться файл `java-keywords.jar`.

Запуск производится с помощью скрипта `run.sh` или через `java -jar`:
```shell
./run.sh -i <path to project> -o <path to output> [-c <path to cache file>] [-t <number of threads>] [-f]
```

```shell
java -jar java-keywords.jar -i <path to project> -o <path to output> [-c <path to cache file>] [-t <number of threads>] [-f]
```

В программе доступен `--help`:
```shell
$> ./run.sh -h
Usage: clargs [OPTIONS]

Options:
  -i, --input TEXT   Input path
  -o, --output TEXT  Output path
  -c, --cache TEXT   Cache path
  -t, --threads INT  Thread count
  -f, --force        Force recalculation without cache
  -h, --help         Show this message and exit
```

Также сборку и запуск можно делать через `gradle build` и `gradle run`.

Программа не учитывает ключевые слова, находящиеся внутри комментариев.