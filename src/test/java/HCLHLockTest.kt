import org.jetbrains.kotlinx.lincheck.LoggingLevel
import org.jetbrains.kotlinx.lincheck.annotations.Operation
import org.jetbrains.kotlinx.lincheck.annotations.Param
import org.jetbrains.kotlinx.lincheck.check
import org.jetbrains.kotlinx.lincheck.paramgen.IntGen
import org.jetbrains.kotlinx.lincheck.strategy.managed.modelchecking.ModelCheckingOptions
import org.junit.Test
import ru.ricnorr.numa.locks.HCLHLock.HCLHLockCore

@Param(name = "clusterID", gen = IntGen::class, conf = "0:2")
class HCLHLockTest {

    private val lock = HCLHLockCore()

    private var counter: Long = 0;

    @Operation
    fun add(@Param(name = "clusterID") clusterID: Int) {
        val myNode = HCLHLockCore.QNodeHCLH()
        val prevNode = lock.lock(myNode, clusterID)
        counter++
        lock.unlock(myNode);
    }

    @Operation
    fun get(@Param(name = "clusterID") clusterID: Int): Long {
        val value: Long
        val myNode = HCLHLockCore.QNodeHCLH()
        val prevNode = lock.lock(myNode, clusterID)
        value = counter
        lock.unlock(myNode);
        return value
    }

    @Test
    fun modelCheckingTest() =
        ModelCheckingOptions().sequentialSpecification(LockStateWithClusters::class.java).invocationsPerIteration(100)
            .actorsPerThread(3).actorsBefore(2).actorsAfter(2).iterations(100).logLevel(LoggingLevel.INFO).threads(4)
            .check(this::class.java)
}
