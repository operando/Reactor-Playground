package com.operando.os.reactor.playground;

import reactor.core.publisher.*;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Main {

    public static void main(String... s) throws InterruptedException {
        Mono<?> mono = Mono.empty();
        Mono.empty().subscribe(System.out::println);
        Mono.just("test").subscribe(System.out::println);
        Mono.create(monoSink -> monoSink.success("test")).subscribe(System.out::println);
        Mono.just("test").map(String::length).subscribe(System.out::println);

        Flux.just(IntStream.range(0, 10).boxed().toArray())
                .subscribe(System.out::println);
        Flux<String> stringFlux = Flux.fromIterable(Stream.of("test", "mugemuge").collect(Collectors.toList()));
        stringFlux.subscribe(System.out::println);
        stringFlux.map(String::toUpperCase).subscribe(System.out::println);

        Flux.create(fluxSink -> {
            IntStream.range(0, 5).forEach(fluxSink::next);
            fluxSink.complete();
        }).subscribe(System.out::println);

        // intervalごとに値が流れ続ける. 0からのLong
        Flux.interval(Duration.ofMillis(100))
                .subscribe(System.out::println);

        Thread.sleep(2000);

        Flux.error(new NullPointerException())
                .subscribe(o -> {
                }, Throwable::printStackTrace);

        // 内部でSchedulerにSchedulers.parallel()が指定される
        Flux.interval(Duration.ofMillis(10))
                .zipWithIterable(() -> Stream.of("test", "hoge").iterator())
                .doOnComplete(() -> System.out.println("doOnComplete"))
                .subscribe(objects -> {
                    System.out.println(objects.getT1());
                    System.out.println(objects.getT2());
                });

        Thread.sleep(1000);

        Flux.interval(Duration.ofMillis(100))
                .zipWith(Flux.just("{A}", "{B}", "{C}"), (i, item) -> "item " + i + ": " + item)
                .subscribe(System.out::println);

        Thread.sleep(1000);

        System.out.println(Flux.just(IntStream.range(0, 5).boxed().toArray()).blockFirst());
        System.out.println(Flux.just(IntStream.rangeClosed(0, 5).boxed().toArray()).blockLast());

        Flux.concat(Mono.just("test"), Mono.just("test2"), Mono.just("test3")).subscribe(System.out::println);

        Flux.concat(Flux.fromStream(IntStream.range(0, 5).boxed())).subscribe(System.out::println);
        Flux.concat(Flux.fromStream(IntStream.range(0, 5).boxed()), Flux.fromStream(IntStream.range(0, 5).boxed())).subscribe(System.out::println);

        Flux.fromStream(IntStream.rangeClosed(5, 10).boxed())
                .concatWith(Flux.fromStream(IntStream.rangeClosed(100, 105).boxed()))
                .subscribe(System.out::println);

        System.out.println(Flux.fromStream(IntStream.rangeClosed(5, 10).boxed()).collectList().block());
        System.out.println(Flux.fromStream(Stream.of(9, 4, 2, 424, 232, 2, 6)).collectSortedList().block());

        // 内部でList<Comparable>にcastしてCollections.sortしてるので、その段階で落ちる
        // Exception in thread "main" java.lang.ClassCastException: com.operando.os.reactor.playground.Main$Test cannot be cast to java.lang.Comparable
        // System.out.println(Flux.fromStream(Stream.of(new Test(), new Test(), new Test())).collectSortedList().block());

        // collectSortedListの引数でsort条件を渡せばOK
        System.out.println(Flux.fromStream(Stream.of(new Test(), new Test(), new Test())).collectSortedList((o1, o2) -> 0).block());

        // return key
        System.out.println(createABCFlux().collectMap(String::toUpperCase).block());
        // return key / value
        System.out.println(createABCFlux().collectMap(String::toUpperCase, String::length).block());

        Thread.sleep(500);

        Mono<String> stringMono = createABCFlux().next();
        stringMono.subscribe(System.out::println);

        Flux.empty().single("default").subscribe(System.out::println);
        Flux.empty().singleOrEmpty().subscribe(System.out::println);

        Mono.just("test").flux().subscribe(System.out::println);
        Mono.just("test").flatMapMany(s1 -> Flux.empty()).subscribe(System.out::println);

        Flux.fromStream(IntStream.rangeClosed(0, 100).boxed())
                .parallel(8) //parallelism
                .runOn(Schedulers.parallel())
                .doOnNext(d -> System.out.println("value " + d + " : I'm on thread " + Thread.currentThread()))
                .sequential()
                .subscribe();

        Thread.sleep(100);

        Flux.fromStream(IntStream.rangeClosed(0, 5).boxed())
                .log()
                .checkpoint("test")
                .subscribe();

        MonoProcessor<String> stringMonoProcessor = MonoProcessor.create();
        stringMonoProcessor.subscribe(System.out::println);
        System.out.println("MonoProcessor");
        stringMonoProcessor.onNext("test");

        UnicastProcessor<String> stringUnicastProcessor = UnicastProcessor.create();
        stringUnicastProcessor.subscribe(System.out::println);
        System.out.println("UnicastProcessor");
        stringUnicastProcessor.onNext("test");
        stringUnicastProcessor.onNext("test2");
        stringUnicastProcessor.onComplete();
        stringUnicastProcessor.onNext("test3");

    }

    public static class Test {
    }

    private static Flux<String> createABCFlux() {
        return Flux.just("a", "b", "c");
    }
}
