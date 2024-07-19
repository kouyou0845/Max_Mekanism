package mekanism.common.lib.distribution;

import java.util.Locale;
import java.util.function.Supplier;
import mekanism.common.lib.distribution.handler.InfiniteIntegerHandler;
import mekanism.common.lib.distribution.handler.IntegerHandler;
import mekanism.common.lib.distribution.handler.PartialIntegerHandler;
import mekanism.common.lib.distribution.handler.SpecificAmountIntegerHandler;
import mekanism.common.lib.distribution.target.IntegerTarget;
import mekanism.common.util.EmitUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Test Distribution via EmitUtils")
class DistributionTest {

    public static IntegerTarget getTargets(int infinite, int some, int none) {
        IntegerTarget target = new IntegerTarget();
        addTargets(target, InfiniteIntegerHandler::new, infinite);
        addTargets(target, PartialIntegerHandler::new, some);
        addTargets(target, () -> new SpecificAmountIntegerHandler(0), none);
        return target;
    }

    private static void addTargets(IntegerTarget targets, Supplier<IntegerHandler> targetSupplier, int count) {
        for (int i = 0; i < count; i++) {
            targets.addHandler(targetSupplier.get());
        }
    }

    @Test
    @DisplayName("Test sending to targets where the amounts divide evenly")
    void testEvenDistribution() {
        int toSend = 10;
        IntegerTarget availableAcceptors = getTargets(toSend, 0, 0);
        Assertions.assertEquals(toSend, EmitUtils.sendToAcceptors(availableAcceptors, toSend, toSend));
    }

    @Test
    @DisplayName("Test sending to non divisible amounts")
    void testRemainderDistribution() {
        int toSend = 10;
        IntegerTarget availableAcceptors = getTargets(7, 0, 0);
        Assertions.assertEquals(toSend, EmitUtils.sendToAcceptors(availableAcceptors, toSend, toSend));
    }

    @Test
    @DisplayName("Test sending to more targets than we have enough to send one to each of")
    void testAllRemainder() {
        int toSend = 3;
        IntegerTarget availableAcceptors = getTargets(7, 0, 0);
        Assertions.assertEquals(toSend, EmitUtils.sendToAcceptors(availableAcceptors, toSend, toSend));
        //TODO: Make it so that we try to send this more evenly initially before falling back to send to remainders?
        /*for (IntegerTarget availableAcceptor : availableAcceptors) {
            System.out.println("Amount accepted: " + availableAcceptor.getAccepted());
        }*/
    }

    @Test
    @DisplayName("Test to check if the remainder is being calculated correctly")
    void testCorrectRemainder() {
        int toSend = 5;
        //Three targets so that we have a split of one and a remainder of two (initial)
        //First one can accept exactly one
        //total to send -> 4, to split among -> 2, to send -> 2 (remainder none)
        IntegerTarget availableAcceptors = new IntegerTarget();
        IntegerHandler handler = new SpecificAmountIntegerHandler(1);
        availableAcceptors.addHandler(handler);
        addTargets(availableAcceptors, () -> new SpecificAmountIntegerHandler(3), 2);
        int sent = EmitUtils.sendToAcceptors(availableAcceptors, toSend, toSend);
        if (sent > toSend) {
            Assertions.fail(String.format(Locale.ROOT, "expected: <%s> to be greater or equal to: <%s>", toSend, sent));
        }
    }
}