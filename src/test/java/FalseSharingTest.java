import org.junit.Test;
import org.openjdk.jol.info.ClassLayout;
import ru.ricnorr.numa.locks.*;

public class FalseSharingTest {
    @Test
    public void MCS() {
        System.out.println(ClassLayout.parseClass(MCS_WITH_PADDING.QNode.class).toPrintable());
    }

    @Test
    public void HMCS_CCL_PLUS_NUMA_HIERARCHY_WITH_PADDING() {
        System.out.println(ClassLayout.parseClass(HMCS_CCL_PLUS_NUMA_HIERARCHY_WITH_PADDING.QNode.class).instanceSize() == 128);
    }

    @Test
    public void HMCS_CCL_PLUS_NUMA_PLUS_SUPERNUMA_HIERARCHY_WITH_PADDING() {
        System.out.println(ClassLayout.parseClass(HMCS_CCL_PLUS_NUMA_PLUS_SUPERNUMA_HIERARCHY_WITH_PADDING.QNode.class).instanceSize() == 128);
    }

    @Test
    public void HMCS_ONLY_CCL_HIERARCHY_WITH_PADDING() {
        System.out.println(ClassLayout.parseClass(HMCS_ONLY_CCL_HIERARCHY_WITH_PADDING.QNode.class).instanceSize() == 128);
    }

    @Test
    public void HMCS_ONLY_NUMA_HIERARCHY_WITH_PADDING() {
        System.out.println(ClassLayout.parseClass(HMCS_ONLY_NUMA_HIERARCHY_WITH_PADDING.QNode.class).instanceSize() == 128);
    }
}
