import org.jetbrains.kotlinx.lincheck.LoggingLevel
import org.jetbrains.kotlinx.lincheck.annotations.Operation
import org.jetbrains.kotlinx.lincheck.annotations.Param
import org.jetbrains.kotlinx.lincheck.check
import org.jetbrains.kotlinx.lincheck.paramgen.IntGen
import org.jetbrains.kotlinx.lincheck.strategy.managed.modelchecking.ModelCheckingOptions
import ru.ricnorr.numa.locks.hclh.AbstractHCLHLock
import ru.ricnorr.numa.locks.hclh.HCLHNodeNoPad

@Param(name = "clusterID", gen = IntGen::class, conf = "0:1")
class HCLHNumaNoPadTest {

    private val lock = AbstractHCLHLock.HCLHLockCore {
        HCLHNodeNoPad()
    }

    private var counter: Long = 0

    @Operation
    fun add(@Param(name = "clusterID") clusterID: Int) {
        val myNode = HCLHNodeNoPad()
        val prevNode = lock.lock(myNode, clusterID)
        counter++
        lock.unlock(myNode)
    }

    @Operation
    fun get(@Param(name = "clusterID") clusterID: Int): Long {
        val value: Long
        val myNode = HCLHNodeNoPad()
        val prevNode = lock.lock(myNode, clusterID)
        value = counter
        lock.unlock(myNode)
        return value
    }

    @org.junit.Test
    fun modelCheckingTest() =
        ModelCheckingOptions().sequentialSpecification(LockStateWithClusters::class.java).invocationsPerIteration(10000)
            .actorsPerThread(4).iterations(100).logLevel(LoggingLevel.INFO).threads(3)
            .check(this::class.java)
}
