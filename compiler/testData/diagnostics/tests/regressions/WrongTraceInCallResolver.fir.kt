open class Foo {}
open class Bar {}

fun <T : Bar, T1> foo(x : Int) {}
fun <T1, T : Foo> foo(x : Long) {}

fun f(): Unit {
    <!INAPPLICABLE_CANDIDATE!>foo<!><Int, Int>(1)
}
