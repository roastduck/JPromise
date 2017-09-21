# JPromise

A Simple Promise like asynchronization library for Java and Android.

[![Build Status](https://travis-ci.org/roastduck/JPromise.svg?branch=master)](https://travis-ci.org/roastduck/JPromise)

## Main Features

- Programming in Java with `.then()` and `.fail()`.
- Throwing exceptions to trigger failures instead of calling `reject()`.
- **(For Android) Running callbacks in background thread or UI thread.**

## Quick Example

```java
import org.jpromise.*;

public class Main {
    public static void main(String[] args)
    {
        new Promise<>(Promise.from((Integer x) -> x * 2 + 1), 2)
                .then(Promise.from((Integer x) -> { System.out.println(x); }));
        // Output = 5
    }
}
```

## Usages

### Create an asynchronous task

Simply create a Promise object, then it will run asynchronous immediately.

With input: `new Promise(callback..., input)`,

Or without input (input == null): `new Promise(callback...)`.

### Chain the asynchronous task with callbacks

Callback in `.then()` will be executed when *the immediate preceding callback* finished successfully. `.then()` will return another `Promise` object so you can chain things up:

```java
new Promise(callback1).then(callback2_executed_after_callback1).then(callback3_executed_after_callback2);
```

Callback in `.fail()` will be executed when *any of the preceding callbacks* throws any `Exception`s, but will only be executed once.  `.fail()` will also return another `Promise` object.

```java
new Promise(callback1).then(callback2).fail(callback3_excuted_after_callback1_or_callback2_throws);
```

You can even chain a `.then()` after a `.fail()`:

```java
.fail(callbackX).then(callbackY_executed_after_callbackX);
```

### Use different types of callbacks

You can use the following 4 types of callback in the constructor, `.then()` or `.fail()`.

Callback with input and output:

```java
new Promise<>(new CallbackIO<IN,OUT>() {
  @Override
  OUT run(IN result) throws Exception { ... }
}, input);
```

Callback with input only:

```java
new Promise<>(new CallbackI<IN>() {
  @Override
  void run(IN result) throws Exception { ... }
}, input);
```

Callback with output only

```java
new Promise<>(new CallbackO<OUT>() {
  @Override
  OUT run() throws Exception { ... }
});
```

Callback without input nor output

```java
new Promise<>(new CallbackV() {
  @Override
  void run() throws Exception { ... }
});
```

### Lambda expression, Runnable or Callable support

Using lambda expression needs a little bit workaround, because Java doesn't support converting instantialize an abstract class (not interface) lambda expression. However, you can use the `Promise.from()` factory method for all the 4 types of callbacks. For example:

```java
new Promise<>(Promise.from((Integer x) -> x + 1), 1);
```

Or

```java
new Promise<>(Promise.from(() -> {}));
```

You can also use Java's Runnable or Callable with `Promise.from(a_runnable)` or `Promise.from(a_callable)`. However, because Runnable or Callable doesn't allow throwing an Exception except for RuntimeException.

### Thread control for Android

On Android, you often want to run some callbacks in the UI thread (the main thread), and others in background threads. When using `Promise` class, all the callback runs in background (implemented with `Executors.newCachedThreadPool()`). However, we provide specialized `AndroidPromise` class for Android, with `.thenUI()` and `.failUI()` to run callbacks in the UI thread. If you still want to run callbacks in the background, just keep using `.then` and `.fail()`. The usage remains the same.

Besides, you can call `.setRunInUI()` to switch a Promise object to run its callback in the UI thread, if it has not yet been run.

### Other functionalities

| Function                                | Explaination                                                                          |
|-----------------------------------------|---------------------------------------------------------------------------------------|
| `Promise.cancel()`                      | Cancel a Promise callback if it has not yet been run                                  |
| `Promise.waitUntilHasRun(long timeout)` | Block the thread until the callback has done. This is especially useful in unit tests. If the time is out, it throws an InterruptedException |
| `Promise.waitUntilHasRun()`             | Another version of `.waitUntilHasRun()` with default timeout                          |
