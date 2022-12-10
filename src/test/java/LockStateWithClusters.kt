import org.jetbrains.kotlinx.lincheck.verifier.VerifierState

class LockStateWithClusters : VerifierState() {
    private var q: Long = 0

    fun add(clusterID: Int) {
        q++;
    }

    fun get(clusterID: Int): Long {
        return q
    }

    override fun extractState() = q
}
