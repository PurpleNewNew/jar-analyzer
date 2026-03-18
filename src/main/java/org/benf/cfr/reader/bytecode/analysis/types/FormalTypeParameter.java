package org.benf.cfr.reader.bytecode.analysis.types;

import org.benf.cfr.reader.bytecode.analysis.types.annotated.JavaAnnotatedTypeInstance;
import org.benf.cfr.reader.entities.annotations.AnnotationTableTypeEntry;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.util.DecompilerComments;
import org.benf.cfr.reader.util.TypeUsageCollectable;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.collections.MapFactory;
import org.benf.cfr.reader.util.output.Dumpable;
import org.benf.cfr.reader.util.output.Dumper;

import java.util.List;
import java.util.Map;

public class FormalTypeParameter implements Dumpable, TypeUsageCollectable {
    private String name;
    private JavaTypeInstance classBound;
    private JavaTypeInstance interfaceBound;

    public FormalTypeParameter(String name, JavaTypeInstance classBound, JavaTypeInstance interfaceBound) {
        this.name = name;
        this.classBound = classBound;
        this.interfaceBound = interfaceBound;
    }

    public static Map<String, FormalTypeParameter> getMap(List<FormalTypeParameter> formalTypeParameters) {
        Map<String, FormalTypeParameter> res = MapFactory.newMap();
        if (formalTypeParameters != null) {
            for (FormalTypeParameter p : formalTypeParameters) {
                res.put(p.getName(), p);
            }
        }
        return res;
    }

    public String getName() {
        return name;
    }

    @Override
    public void collectTypeUsages(TypeUsageCollector collector) {
        collector.collect(classBound);
        collector.collect(interfaceBound);
    }

    public void add(FormalTypeParameter other) {
        JavaTypeInstance typ = getBound();
        JavaTypeInstance otherTyp = other.getBound();
        if (typ instanceof JavaIntersectionTypeInstance) {
            typ = ((JavaIntersectionTypeInstance) typ).withPart(otherTyp);
        } else {
            typ = new JavaIntersectionTypeInstance(ListFactory.newList(typ, otherTyp));
        }
        if (classBound != null) {
            classBound = typ;
        } else {
            interfaceBound = typ;
        }
    }

    public JavaTypeInstance getBound() {
        return classBound == null ? interfaceBound : classBound;
    }

    @Override
    public Dumper dump(Dumper d) {
        d.print(name);
        dumpBounds(d, classBound, interfaceBound);
        return d;
    }

    // TODO: This really shouldn't be at display time.
    public Dumper dump(Dumper d, List<AnnotationTableTypeEntry> typeAnnotations, List<AnnotationTableTypeEntry> typeBoundAnnotations) {
        if (!typeAnnotations.isEmpty()) {
            typeAnnotations.get(0).dump(d);
            d.print(' ');
        }
        d.print(name);
        JavaTypeInstance annotatedPrimary = classBound == null ? interfaceBound : classBound;
        if (annotatedPrimary != null) {
            JavaAnnotatedTypeInstance ati = annotatedPrimary.getAnnotatedInstance();
            DecompilerComments comments = new DecompilerComments();
            TypeAnnotationHelper.apply(ati, typeBoundAnnotations, comments);
            d.dump(comments);
            dumpAnnotatedBounds(d, classBound, ati, classBound == null ? null : interfaceBound);
        }
        return d;
    }

    private static void dumpBounds(Dumper d, JavaTypeInstance classBound, JavaTypeInstance interfaceBound) {
        boolean printedBound = false;
        if (classBound != null && !TypeConstants.objectName.equals(classBound.getRawName())) {
            d.print(" extends ").dump(classBound);
            printedBound = true;
        }
        if (interfaceBound != null) {
            d.print(printedBound ? " & " : " extends ").dump(interfaceBound);
        }
    }

    private static void dumpAnnotatedBounds(Dumper d,
                                            JavaTypeInstance rawClassBound,
                                            JavaAnnotatedTypeInstance annotatedClassBound,
                                            JavaTypeInstance interfaceBound) {
        boolean printedBound = false;
        if (annotatedClassBound != null && rawClassBound != null && !TypeConstants.objectName.equals(rawClassBound.getRawName())) {
            d.print(" extends ").dump(annotatedClassBound);
            printedBound = true;
        }
        if (interfaceBound != null) {
            d.print(printedBound ? " & " : " extends ").dump(interfaceBound);
        }
    }

    @Override
    public String toString() {
        return name + " [ " + classBound + "|" + interfaceBound + "]";
    }

}
