import org.jetbrains.kotlinx.lincheck.LoggingLevel
import org.jetbrains.kotlinx.lincheck.annotations.Operation
import org.jetbrains.kotlinx.lincheck.annotations.Param
import org.jetbrains.kotlinx.lincheck.check
import org.jetbrains.kotlinx.lincheck.paramgen.IntGen
import org.jetbrains.kotlinx.lincheck.strategy.managed.modelchecking.ModelCheckingOptions
import org.junit.Test
import ru.ricnorr.numa.locks.basic.CLH

@Param(name = "clusterID", gen = IntGen::class, conf = "0:2")
class CLHTest {

    private val lock = CLH()

    private var counter: Long = 0

    @Operation
    fun add() {
        val obj = lock.lock(null)
        counter++
        lock.unlock(obj)
    }

    @Operation
    fun get(): Long {
        val value: Long
        val obj = lock.lock(null)
        value = counter
        lock.unlock(obj)
        return value
    }

    @Test
    fun modelCheckingTest() =
        ModelCheckingOptions().sequentialSpecification(LockStateNoClusters::class.java).invocationsPerIteration(100)
            .actorsPerThread(3).actorsBefore(2).actorsAfter(2).iterations(100).logLevel(LoggingLevel.INFO).threads(4)
            .check(this::class.java)
}
