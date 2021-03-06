package org.kodein.di.generic

import org.kodein.di.Kodein
import org.kodein.di.direct
import org.kodein.di.test.*
import kotlin.test.*

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class GenericJvmTests_07_Error {

    @Test
    fun test_00_DependencyLoop() {

        val kodein = Kodein {
            bind<A>() with singleton { A(instance()) }
            bind<B>() with singleton { B(instance()) }
            bind<C>() with singleton { C(instance()) }
        }

        val ex = assertFailsWith<Kodein.DependencyLoopException> {
            kodein.direct.instance<A>()
        }

        assertEquals("""
Dependency recursion:
     bind<A>()
    ╔╩>bind<B>()
    ║  ╚>bind<C>()
    ║    ╚>bind<A>()
    ╚══════╝
            """.trim(), ex.message
        )
    }

    @Suppress("unused")
    class Recurs0(val a: RecursA)
    @Suppress("unused")
    class RecursA(val b: RecursB)
    @Suppress("unused")
    class RecursB(val c: RecursC)
    @Suppress("unused")
    class RecursC(val a: RecursA)

    @Test fun test_01_RecursiveDependencies() {

        val kodein = Kodein {
            bind() from provider { Recurs0(instance()) }
            bind() from provider { RecursA(instance()) }
            bind() from provider { RecursB(instance(tag = "yay")) }
            bind(tag = "yay") from provider { RecursC(instance()) }
        }

        assertFailsWith<Kodein.DependencyLoopException> {
            kodein.direct.instance<Recurs0>()
        }
    }

    @Test
    fun test_02_NoDependencyLoop() {

        val kodein = Kodein {
            bind<A>() with singleton { A(instance()) }
            bind<A>(tag = "root") with singleton { A(null) }
            bind<B>() with singleton { B(instance()) }
            bind<C>() with singleton { C(instance(tag = "root")) }
        }

        val a by kodein.instance<A>()
        assertNotNull(a.b?.c?.a)
    }

    @Test
    fun test_03_TypeNotFound() {

        val kodein = Kodein.direct {}

        assertFailsWith<Kodein.NotFoundException> {
            kodein.instance<Person>()
        }

        assertFailsWith<Kodein.NotFoundException> {
            kodein.instance<FullName>()
        }

        assertFailsWith<Kodein.NotFoundException> {
            kodein.instance<List<*>>()
        }

        assertFailsWith<Kodein.NotFoundException> {
            kodein.instance<List<String>>()
        }
    }

    @Test
    fun test_04_NameNotFound() {

        val kodein = Kodein.direct {
            bind<Person>() with provider { Person() }
            bind<Person>(tag = "named") with provider { Person("Salomon") }
        }

        assertFailsWith<Kodein.NotFoundException> {
            kodein.instance<Person>(tag = "schtroumpf")
        }
    }

    @Test
    fun test_05_FactoryIsNotProvider() {

        val kodein = Kodein.direct {
            bind<Person>() with factory { name: String -> Person(name) }
        }

        assertFailsWith<Kodein.NotFoundException> {
            kodein.provider<Person>()
        }
    }

    @Test
    fun test_06_ProviderIsNotFactory() {

        val kodein = Kodein.direct {
            bind<Person>() with provider { Person() }
        }

        assertFailsWith<Kodein.NotFoundException> {
            kodein.factory<Int, Person>()
        }
    }

    @Test
    fun test_07_BindFromUnit() {

        fun unit(@Suppress("UNUSED_PARAMETER") i: Int = 42) {}

        val kodein = Kodein.direct {
            assertFailsWith<IllegalArgumentException> {
                bind() from factory { i: Int -> unit(i) }
            }
            assertFailsWith<IllegalArgumentException> {
                bind() from provider { unit() }
            }
            assertFailsWith<IllegalArgumentException> {
                bind() from instance(Unit)
            }
            assertFailsWith<IllegalArgumentException> {
                bind() from singleton { unit() }
            }

            bind<Unit>() with instance(unit())
        }

        assertSame(Unit, kodein.instance())
    }
}
