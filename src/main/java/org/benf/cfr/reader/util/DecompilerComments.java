package org.benf.cfr.reader.util;

import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.output.Dumpable;
import org.benf.cfr.reader.util.output.Dumper;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DecompilerComments implements Dumpable {
    private Set<DecompilerComment> comments = SetFactory.newOrderedSet();

    public DecompilerComments() {
    }

    public void addComment(String comment) {
        DecompilerComment decompilerComment = new DecompilerComment(comment);
        comments.add(decompilerComment);
    }

    public void addComment(DecompilerComment comment) {
        comments.add(comment);
    }

    public void addComments(Collection<DecompilerComment> comments) {
        this.comments.addAll(comments);
    }

    public boolean removeComment(DecompilerComment comment) {
        return comments.remove(comment);
    }

    @Override
    public Dumper dump(Dumper d) {
        if (comments.isEmpty()) return d;
        d.beginBlockComment(false);
        for (DecompilerComment comment : comments) {
            d.dump(comment);
        }
        d.endBlockComment();
        return d;
    }

    public boolean contains(DecompilerComment comment) {
        return comments.contains(comment);
    }

    public Collection<DecompilerComment> getCommentCollection() {
        return comments;
    }

    public DecompilationQuality getQuality() {
        DecompilationQuality quality = DecompilationQuality.CLEAN;
        for (DecompilerComment comment : comments) {
            quality = DecompilationQuality.max(quality, comment.getQuality());
        }
        return quality;
    }

    public boolean isDegraded() {
        return getQuality() != DecompilationQuality.CLEAN;
    }

    public List<DecompilerComment> getDegradingComments() {
        return comments.stream()
                .filter(DecompilerComment::isDegraded)
                .collect(Collectors.toList());
    }

}
