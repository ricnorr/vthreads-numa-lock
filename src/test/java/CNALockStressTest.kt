import org.jetbrains.kotlinx.lincheck.LinChecker
import org.jetbrains.kotlinx.lincheck.LoggingLevel
import org.jetbrains.kotlinx.lincheck.annotations.Operation
import org.jetbrains.kotlinx.lincheck.annotations.Param
import org.jetbrains.kotlinx.lincheck.paramgen.IntGen
import org.jetbrains.kotlinx.lincheck.strategy.stress.StressCTest
import org.jetbrains.kotlinx.lincheck.strategy.stress.StressOptions
import org.jetbrains.kotlinx.lincheck.verifier.VerifierState
import ru.ricnorr.numa.locks.cna.AbstractCNA.CNALockCore
import ru.ricnorr.numa.locks.cna.CNANode

@Param(name = "clusterID", gen = IntGen::class, conf = "0:2")
@StressCTest
class CNALockStressTest : VerifierState() {
    private val lock = CNALockCore<CNANode>()

    private var counter: Long = 0

    @Operation
    fun add(@Param(name = "clusterID") clusterID: Int) {
        val me = CNANode()
        me.setSocketAtomically(clusterID)
        lock.lock(me)
        counter++
        lock.unlock(me)
    }

    @Operation
    fun get(@Param(name = "clusterID") clusterID: Int): Long {
        val value: Long
        val me = CNANode()
        me.setSocketAtomically(clusterID)
        lock.lock(me)
        value = counter
        lock.unlock(me)
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
