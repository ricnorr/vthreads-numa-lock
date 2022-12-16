import org.jetbrains.kotlinx.lincheck.LoggingLevel
import org.jetbrains.kotlinx.lincheck.annotations.Operation
import org.jetbrains.kotlinx.lincheck.annotations.Param
import org.jetbrains.kotlinx.lincheck.check
import org.jetbrains.kotlinx.lincheck.paramgen.IntGen
import org.jetbrains.kotlinx.lincheck.strategy.managed.modelchecking.ModelCheckingOptions
import org.junit.Test
import ru.ricnorr.numa.locks.CNALock
import ru.ricnorr.numa.locks.CNALock.CNANode

@Param(name = "clusterID", gen = IntGen::class, conf = "0:1")
class CNALockTest {

    private val lock = CNALock.CNALockCore()

    private var counter: Long = 0;

    @Operation
    fun add(@Param(name = "clusterID") clusterID: Int) {
        val me = CNANode()
        lock.lock(me, clusterID)
        counter++
        lock.unlock(me, clusterID)
    }

    @Operation
    fun get(@Param(name = "clusterID") clusterID: Int): Long {
        val value: Long
        val me = CNANode()
        lock.lock(me, clusterID)
        value = counter
        lock.unlock(me, clusterID)
        return value
    }

    @Test
    fun modelCheckingTest() =
        ModelCheckingOptions().sequentialSpecification(LockStateWithClusters::class.java).invocationsPerIteration(10000)
            .actorsPerThread(4).actorsBefore(3).actorsAfter(3).iterations(100).logLevel(LoggingLevel.INFO).threads(4)
            .check(this::class.java)
}
