import org.jetbrains.kotlinx.lincheck.verifier.VerifierState

class LockStateNoClusters : VerifierState() {
    private var q: Long = 0

    fun add() {
        q++;
    }

    fun get(): Long {
        return q
    }

    override fun extractState() = q
}
