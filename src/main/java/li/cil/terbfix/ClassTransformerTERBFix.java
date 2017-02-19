package li.cil.terbfix;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.FMLLog;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nullable;
import java.util.Objects;

public class ClassTransformerTERBFix implements IClassTransformer {
    @Override
    public byte[] transform(final String name, final String transformedName, final byte[] basicClass) {
        if (Objects.equals(transformedName, "net.minecraft.tileentity.TileEntity")) {
            log("Found TileEntity class, looking for getRenderBoundingBox.");
            return transformTileEntity(basicClass);
        }
        return basicClass;
    }

    private byte[] transformTileEntity(final byte[] basicClass) {
        final MethodInfo methodInfo = new MethodInfo(
                "getRenderBoundingBox",
                "", // Patched in by Forge, never obfuscated.
                "",
                "()Lnet/minecraft/util/math/AxisAlignedBB;",
                "()Lbby;");

        final ClassReader classReader = new ClassReader(basicClass);
        final ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);

        final MethodNode methodNode = findMethod(classNode, methodInfo);
        if (methodNode == null) {
            return basicClass;
        }

        log("Found getRenderBoundingBox, looking for patch location.");
        if (!transformGetRenderBoundingBox(methodNode)) {
            log("Failed patching getRenderBoundingBox!");
            return basicClass;
        }
        log("Successfully patched getRenderBoundingBox!");

        final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private boolean transformGetRenderBoundingBox(final MethodNode methodNode) {
        final MethodInsnInfo patchAfter = new MethodInsnInfo(
                "net/minecraft/block/state/IBlockState",
                "ars",
                "getCollisionBoundingBox",
                "func_185890_d",
                "d",
                "(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/math/AxisAlignedBB;",
                "(Laid;Lcm;)Lbby;");

        for (AbstractInsnNode insn = methodNode.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getType() != AbstractInsnNode.METHOD_INSN) {
                continue;
            }
            final MethodInsnNode minsn = (MethodInsnNode) insn;

            if (minsn.getOpcode() != Opcodes.INVOKEINTERFACE || !patchAfter.matches(minsn)) {
                continue;
            }

            log("Found patch location, attempting to patch.");

            // Skip one ALOAD (BlockPos).
            AbstractInsnNode head = minsn.getNext();
            if (head == null || head.getType() != AbstractInsnNode.VAR_INSN || head.getOpcode() != Opcodes.ALOAD) {
                return false;
            }

            // Remove 2x ALOAD + 3x INVOKEVIRTUAL + 3x I2D (getX, getY and getZ).
            final int removeCount = 8;
            for (int i = 0; i < removeCount; ++i) {
                final AbstractInsnNode next = head.getNext();
                if (next == null) {
                    return false;
                }
                methodNode.instructions.remove(next);
            }

            // Adjust INVOKEVIRTUAL (addCoord to offset)
            head = head.getNext();
            if (head == null || head.getType() != AbstractInsnNode.METHOD_INSN) {
                return false;
            }

            final MethodInfo addCoord = new MethodInfo(
                    "addCoord",
                    "func_72321_a",
                    "a",
                    "(DDD)Lnet/minecraft/util/math/AxisAlignedBB;",
                    "(DDD)Lbby;");
            final MethodInfo offset = new MethodInfo(
                    "offset",
                    "func_186670_a",
                    "a",
                    "(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/math/AxisAlignedBB;",
                    "(Lcm;)Lbby;");
            return transformMethod((MethodInsnNode) head, addCoord, offset);
        }

        return false;
    }

    @Nullable
    private static MethodNode findMethod(final ClassNode classNode, final MethodInfo methodInfo) {
        for (final MethodNode methodNode : classNode.methods) {
            if (methodInfo.matches(methodNode)) {
                return methodNode;
            }
        }
        return null;
    }

    private static boolean transformMethod(final MethodInsnNode toAdjust, final MethodInfo from, final MethodInfo to) {
        if (Objects.equals(toAdjust.name, from.mcpName)) {
            toAdjust.name = to.mcpName;
        } else if (Objects.equals(toAdjust.name, from.srgName)) {
            toAdjust.name = to.srgName;
        } else if (Objects.equals(toAdjust.name, from.obfName)) {
            toAdjust.name = to.obfName;
        } else {
            return false;
        }

        if (Objects.equals(toAdjust.desc, from.srgDesc)) {
            toAdjust.desc = to.srgDesc;
        } else if (Objects.equals(toAdjust.desc, from.obfDesc)) {
            toAdjust.desc = to.obfDesc;
        } else {
            return false;
        }

        return true;
    }

    private static void log(final String message) {
        FMLLog.info("[TERBFix] %s", message);
    }

    private static class MethodInfo {
        final String mcpName, srgName, obfName, srgDesc, obfDesc;

        MethodInfo(final String mcpName, final String srgName, final String obfName, final String srgDesc, final String obfDesc) {
            this.mcpName = mcpName;
            this.srgName = srgName;
            this.obfName = obfName;
            this.srgDesc = srgDesc;
            this.obfDesc = obfDesc;
        }

        boolean matches(final MethodNode node) {
            final boolean nameMatches = Objects.equals(node.name, mcpName) || Objects.equals(node.name, obfName) || Objects.equals(node.name, srgName);
            final boolean descMatches = Objects.equals(node.desc, srgDesc) || Objects.equals(node.desc, obfDesc);
            return nameMatches && descMatches;
        }
    }

    private static class MethodInsnInfo extends MethodInfo {
        private final String srgClassName, obfClassName;

        MethodInsnInfo(final String srgClassName, final String obfClassName, final String mcpName, final String srgName, final String obfName, final String srgDesc, final String obfDesc) {
            super(mcpName, srgName, obfName, srgDesc, obfDesc);
            this.srgClassName = srgClassName;
            this.obfClassName = obfClassName;
        }

        boolean matches(final MethodInsnNode node) {
            final boolean ownerMatches = Objects.equals(node.owner, srgClassName) || Objects.equals(node.owner, obfClassName);
            final boolean nameMatches = Objects.equals(node.name, mcpName) || Objects.equals(node.name, obfName) || Objects.equals(node.name, srgName);
            final boolean descMatches = Objects.equals(node.desc, srgDesc) || Objects.equals(node.desc, obfDesc);
            return ownerMatches && nameMatches && descMatches;
        }
    }
}
