import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.css.*
import kotlinx.html.InputType
import kotlinx.html.classes
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onKeyDownFunction
import kotlinx.serialization.DynamicObjectParser
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.Serializable
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import react.*
import react.dom.*
import styled.css
import styled.styledInput
import styled.styledP
import styled.styledSpan
import kotlin.browser.document
import kotlin.browser.window
import kotlin.random.Random
import kotlin.reflect.KClass

/**
 * Exercises:
 *   - Basic       8 - 5 = ?
 *   - Missing     8 - ? = 2
 */


interface ExerciseProps : RProps {
    var a: Int
    var op: Operation
    var b: Int
    var onSuccess: (action: ExerciseState) -> Any
}

interface ExerciseState : RState {
    var triesLeft: Int
    var actual: String
    var success: Boolean
    var resultInput: HTMLInputElement
}

fun RBuilder.catPicture() = img {
    attrs.src = "https://cataas.com/cat?${Random.nextInt(0, 10000000)}"
}

val cry = "\uD83D\uDE22"

val Ticker = functionalComponent<RProps> {
    println("HERE")
    val (tick, setTick) = useState(0)
    window.setTimeout({
        setTick(tick + 1)
    }, 1000)
    div { +"Tick: $tick" }
}

@Serializable
data class TodoItem(val userId: Int, val title: String)

val RestTodo = functionalComponent<RProps> {
    val (title, setTitle) = useState("")

    if (title.isBlank()) {
        val mainScope = MainScope()
        mainScope.launch {
            @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
            val info = window.fetch("https://jsonplaceholder.typicode.com/todos/1")
                .await()
                .json()
                .await()
                .cast(TodoItem::class)

            console.log(info)
            setTitle(info.title)
        }
    }

    div { +"Todo Title: $title" }
}

@OptIn(ImplicitReflectionSerializer::class)
private inline fun <reified T : Any> Any?.cast(_kClass: KClass<T>): T {
    return DynamicObjectParser().parse<T>(this)
}

class Exercise : RComponent<ExerciseProps, ExerciseState>() {
    lateinit var resultInput: HTMLInputElement

    init {
        state.triesLeft = 2
        state.actual = ""
        state.success = false
    }

    override fun componentDidMount() {
        resultInput.focus()
    }

    override fun RBuilder.render() {
        div {
            child(Ticker)
            child(RestTodo)

            if (state.triesLeft == 0) {
                h1 { +cry }
                window.setTimeout({
                    window.location.href = window.location.href
                }, 3000)
                return@div
            }

            if (state.success) {
                catPicture()
                return@div
            }

            attrs.classes = setOf("exercise")

            h1 {
                +"Úkol (máš ${state.triesLeft} ${if (state.triesLeft == 1) "pokus" else "pokusy"}):"
            }

            styledP {
                css {
                    textAlign = TextAlign.center
                }
                styledSpan {
                    css {
                        color = Color.red
                    }
                    +"${props.a}"
                }
                span { +props.op.value }
                styledSpan {
                    css {
                        color = Color.green
                    }
                    +"${props.b}"
                }
                styledSpan {
                    css {
                        color = Color.blue
                    }
                    +"="
                }
                styledInput {
                    css {
                        textAlign = TextAlign.center
                        fontSize = 550.pct
                    }
                    ref { resultInput = it }
                    attrs {
                        size = "2"
                        maxLength = "2"
                        minLength = "1"
                        pattern = "[0-1]+"
                        placeholder = "?"
                        type = InputType.text
                        onChangeFunction = ::onChange
                        onKeyDownFunction = { event ->
                            val key = event.asDynamic().key as String
                            if (key == "Enter") {
                                onSubmit()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onChange(event: Event) {
        val newValue = (event.target as HTMLInputElement).value
        setState { actual = newValue }
    }

    private fun onSubmit() {
        val expected = props.op.compute(props.a, props.b)
        val actual = state.actual.toIntOrNull()

        if (actual == expected) {
            props.onSuccess(state)
        } else {
            resultInput.value = cry
            window.setTimeout({ resultInput.value = "" }, 1500)
            setState {
                this.triesLeft = triesLeft - 1
            }
        }
    }
}

enum class Operation(val value: String) {
    PLUS("+") {
        override fun compute(a: Int, b: Int): Int = a + b
    },
    MINUS("-") {
        override fun compute(a: Int, b: Int): Int = a - b
    };

    abstract fun compute(a: Int, b: Int): Int
}

fun main() {
    fun onExerciseSuccess(exerciseState: @ParameterName(name = "action") ExerciseState) {
        render(document.getElementById("root")) {
            catPicture()
            br { }
            br { }

            a {
                attrs.href = window.location.href
                +"Chci další úkol.."
            }
        }
    }

    val a = (0..10).random()
    val b = (0..a).random()

    render(document.getElementById("root")) {
        child(Exercise::class) {
            attrs {
                this.a = a
                op = Operation.MINUS
                this.b = b
                onSuccess = ::onExerciseSuccess
            }
        }
    }
}
