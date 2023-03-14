import org.jetbrains.kotlinx.lincheck.LinChecker
import org.jetbrains.kotlinx.lincheck.LoggingLevel
import org.jetbrains.kotlinx.lincheck.annotations.Operation
import org.jetbrains.kotlinx.lincheck.annotations.Param
import org.jetbrains.kotlinx.lincheck.paramgen.IntGen
import org.jetbrains.kotlinx.lincheck.strategy.stress.StressCTest
import org.jetbrains.kotlinx.lincheck.strategy.stress.StressOptions
import org.jetbrains.kotlinx.lincheck.verifier.VerifierState
import ru.ricnorr.numa.locks.hclh.AbstractHCLHLock.HCLHLockCore
import ru.ricnorr.numa.locks.hclh.HCLHNodeNoPad

@Param(name = "clusterID", gen = IntGen::class, conf = "0:2")
@StressCTest
class HCLHLockStressTest : VerifierState() {
    private val lock = HCLHLockCore {
        HCLHNodeNoPad()
    }

    private var counter: Long = 0

    @Operation
    fun add(@Param(name = "clusterID") clusterID: Int) {
        val node = HCLHNodeNoPad()
        lock.lock(node, clusterID)
        counter++
        lock.unlock(node)
    }

    @Operation
    fun get(@Param(name = "clusterID") clusterID: Int): Long {
        val value: Long
        val node = HCLHNodeNoPad()
        lock.lock(node, clusterID)
        value = counter
        lock.unlock(node)
        return value
    }

    override fun extractState(): Any {
        return counter
    }

    @org.junit.Test
    fun test() {
        val opts = StressOptions()
            .iterations(100)
            .threads(3)
            .logLevel(LoggingLevel.INFO)
        LinChecker.check(CNALockStressTest::class.java, opts)
    }
}
