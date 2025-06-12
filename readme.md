Optional'in getirdiği ağır yükten ve exception handling'in Stream'in pipeline'i içerisinde yazılan try/catch
block'larının, Stream'in genel amaçlarına ters düşmesinden dolayı, best practice olarak Record kullanılabilir;

Result type'ının temel amacı, olası bir değeri veya başarılı olunamazsa başarısızlığın nedenini represent eden bir
`Exception`'ı tutmaktır.

```
public record Result<V, E extends Throwable>(V value, E throwable, boolean isSuccess) {
    public static <V, E extends Throwable> Result<V, E> isSuccess(V value) {
        return new Result<>(value, null, true);
    }

    public static <V, E extends Throwable> Result<V, E> failure(E throwable) {
        return new Result<>(null, throwable, false);
    }

    static Result<String, IOException> safeReadString(Path path) {
        try{
            return Result.isSuccess(Files.readString(path));
        } catch (IOException ex){
            return Result.failure(ex);
        }
    }
}
```

Record component'leri farklı state'leri reflect eder. Explicit `success` field'i, başarılı bir operation'ı daha iyi
belirlemeye ve `null`'u geçerli bir value olarak desteklemeye yardımcı olur. Kolaylık sağlayan factory metotları daha
anlamlı bir API sunar. Bu basit iskele `(scaffold)` bile, uygun result'lar oluşturmak için anlamlı bir yol olan
convenience factory metotlarıyla, `Optional` kullanmaya göre belirli bir iyileştirme sunar.

Basitçe bir main method içerisinde kullanılabilir;

```
public static void main(String[] args) {
    long result = Stream.of(Paths.get("war-and-peace.txt"),
                    Paths.get("invalid"),
                    Paths.get("example-10-6/result-as-return-type.jsh"))
            .map(Result::safeReadString)
            .filter(Result::success)
            .count();
    
    System.out.println(result); // 1
}
```

Yeni tip, bir Stream pipeline'ında bir `Optional` kadar kolayca kullanılabilir. Ancak asıl güç, `success` state'ine
bağlı higher-order function'lar ekleyerek ona daha fazla işlevsel özellik kazandırmaktan gelir. Optional type'ının genel
özellikleri, Result type'ını daha da geliştirme konusunda ilham kaynağıdır ve şunları içerir:

* Value'sunu veya Exception'ını transforming.

* Bir Exception'a tepki verme.

* Fallback bir value sağlama

Value veya throwable field'ini transforming, dedicated `map` metotları veya her iki use case'i aynı anda handle etmek
için combined bir metot gerektirir.

```
public record Result<V, E extends Throwable>(V value, E throwable, boolean isSuccess) {
    public static <V, E extends Throwable> Result<V, E> success(V value) {
        return new Result<>(value, null, true);
    }

    public static <V, E extends Throwable> Result<V, E> failure(E throwable) {
        return new Result<>(null, throwable, false);
    }

    <R> Optional<R> mapSuccess(Function<V, R> fn) {
        return this.isSuccess ? Optional.ofNullable(this.value).map(fn)
                : Optional.empty();
    }

    <R> Optional<R> mapFailure(Function<E, R> fn) {
        return this.isSuccess ? Optional.empty()
                : Optional.ofNullable(this.throwable).map(fn);
    }

    <R> R map(Function<V, R> successFn, Function<E, R> failureFn) {
        return this.isSuccess ? successFn.apply(this.value)
                : failureFn.apply(this.throwable);
    }

    static Result<String, IOException> safeReadString(Path path) {
        try {
            return Result.success(Files.readString(path));
        } catch (IOException ex) {
            return Result.failure(ex);
        }
    }
}
```

Singular mapping metotları `(mapSuccess - mapFailure)` oldukça benzerdir ve ilgili result'ı, success'i veya failure'ı
transform eder. İşte bu yüzden her ikisi de concrete bir değer yerine bir `Optional` döndürmelidir. Combined bir `map`
metodu, hem `success` hem de `failure` case'lerini tek bir call'da handle etmenize olanak tanır. Her iki state'de de
handle edildiği için, bir Optional yerine concrete bir değer döndürülür.

`Derived.java`

```
// HANDLE ONLY SUCCESS CASE
List<String> list = Stream.of(Paths.get("war-and-peace.txt"))
        .map(Result::safeReadString)
        .map(result -> result.mapSuccess(String::toUpperCase))
        .flatMap(Optional::stream)
        .toList();
System.out.println(list); // => war-and-peace.txt icerisinde ki tum kelimeler UpperCase
```

Hem success hemde failure case'i için;

```
public static void main(String[] args) {
    String result = Result.safeReadString(Paths.get("war-and.txt"))
            .map(String::toUpperCase, failure -> "IO Error : " + failure);
    
    System.out.println(result); // => IO Error : java.nio.file.NoSuchFileException: war-and.txt
}
```

Ayrıca, value'sunu veya Exception'ını öncelikle tranform etmeyi gerektirmeden bir Result ile çalışmanın bir yolu olması
gerekir. Belirli bir state'e tepki vermek için, hadi Record'umuz içerisine `ifSuccess`, `ifFailure` ve `handle`
metotlarını ekleyelim:

```
public void ifSuccess(Consumer<? super V> action) {
    if (this.isSuccess)
        action.accept(this.value);

}

public void ifFailure(Consumer<? super E> action) {
    if (!this.isSuccess)
        action.accept(this.throwable);
}

public void handle(Consumer<? super V> successAction, Consumer<? super E> failureAction){
    if (this.isSuccess)
        successAction.accept(this.value);
    else
        failureAction.accept(this.throwable);
}
```

Implementation, bir `Function` yerine bir `Consumer` kullanmaları dışında, `mapper` metotlarına neredeyse eşdeğerdir. Bu
iki ekleme yalnızca side-effect'lidir ve bu nedenle `purest` anlamda çok functional değildir. Yine de, bu tür
eklemeler, imperative ve functional yaklaşımlar arasında mükemmel bir geçici çözüm sağlar.

`Derived.java`

```
public static void main(String[] args) {
    Result.safeReadString(Paths.get("war-and-peace.txt"))
            .handle(
                    System.out::println, // -> success case'inde
                    failure -> System.out.println("IO-Error : " + failure.getMessage())
            );
}
```