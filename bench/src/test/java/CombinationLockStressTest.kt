// import io.github.ricnorr.benchmarks.LockType
// import io.github.ricnorr.numa_locks.combination.CombinationLock
// import org.jetbrains.kotlinx.lincheck.LinChecker
// import org.jetbrains.kotlinx.lincheck.LoggingLevel
// import org.jetbrains.kotlinx.lincheck.annotations.Operation
// import org.jetbrains.kotlinx.lincheck.annotations.Param
// import org.jetbrains.kotlinx.lincheck.paramgen.IntGen
// import org.jetbrains.kotlinx.lincheck.strategy.stress.StressCTest
// import org.jetbrains.kotlinx.lincheck.strategy.stress.StressOptions
// import org.jetbrains.kotlinx.lincheck.verifier.VerifierState
//
// @Param(name = "clusterID", gen = IntGen::class, conf = "0:2")
// @StressCTest
// class CombinationLockStressTest : VerifierState() {
//    private val lock = CombinationLock.CombinationLockCore(
//        listOf(
//            CombinationLock.CombinationLockLevelDescription(
//                LockType.MCS,
//                3,
//            ),
//            CombinationLock.CombinationLockLevelDescription(
//                LockType.MCS,
//                0,
//            ),
//        ),
//    )
//
//    private var counter: Long = 0
//
//    @Operation
//    fun add(@Param(name = "clusterID") clusterID: Int) {
//        lock.lock(clusterID)
//        counter++
//        lock.unlock(clusterID)
//    }
//
//    @Operation
//    fun get(@Param(name = "clusterID") clusterID: Int): Long {
//        val value: Long
//        lock.lock(clusterID)
//        value = counter
//        lock.unlock(clusterID)
//        return value
//    }
//
//    override fun extractState(): Any {
//        return counter
//    }
//
//    @org.junit.Test
//    fun test() {
//        val opts = StressOptions().iterations(100).threads(3).logLevel(LoggingLevel.INFO)
//        LinChecker.check(CNALockStressTest::class.java, opts)
//    }
// }
