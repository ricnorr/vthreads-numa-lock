import org.junit.Test;
import org.openjdk.jol.info.ClassLayout;
import ru.ricnorr.numa.locks.basic.MCS;

public class FalseSharingTest {
    @Test
    public void MCS() {
        System.out.println(ClassLayout.parseClass(MCS.QNode.class).toPrintable());
    }
}
