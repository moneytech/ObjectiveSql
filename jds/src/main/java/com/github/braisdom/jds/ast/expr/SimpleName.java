/*
 * Copyright (C) 2007-2010 Júlio Vilmar Gesser.
 * Copyright (C) 2011, 2013-2020 The JavaParser Team.
 *
 * This file is part of JavaParser.
 *
 * JavaParser can be used either under the terms of
 * a) the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * b) the terms of the Apache License
 *
 * You should have received a copy of both licenses in LICENCE.LGPL and
 * LICENCE.APACHE. Please refer to those files for details.
 *
 * JavaParser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 */
package com.github.braisdom.jds.ast.expr;

import com.github.braisdom.jds.ast.AllFieldsConstructor;
import com.github.braisdom.jds.ast.Node;
import com.github.braisdom.jds.ast.nodeTypes.NodeWithIdentifier;
import com.github.braisdom.jds.ast.observer.ObservableProperty;
import com.github.braisdom.jds.ast.visitor.GenericVisitor;
import com.github.braisdom.jds.ast.visitor.VoidVisitor;
import static com.github.braisdom.jds.utils.Utils.assertNonEmpty;
import com.github.braisdom.jds.ast.visitor.CloneVisitor;
import com.github.braisdom.jds.metamodel.NonEmptyProperty;
import com.github.braisdom.jds.metamodel.SimpleNameMetaModel;
import com.github.braisdom.jds.metamodel.JavaParserMetaModel;
import com.github.braisdom.jds.TokenRange;
import com.github.braisdom.jds.ast.Generated;

/**
 * A name that consists of a single identifier.
 * In other words: it.does.NOT.contain.dots.
 *
 * @see Name
 */
public class SimpleName extends Node implements NodeWithIdentifier<SimpleName> {

    @NonEmptyProperty
    private String identifier;

    public SimpleName() {
        this(null, "empty");
    }

    @AllFieldsConstructor
    public SimpleName(final String identifier) {
        this(null, identifier);
    }

    /**
     * This constructor is used by the parser and is considered private.
     */
    @Generated("com.github.braisdom.jds.generator.core.node.MainConstructorGenerator")
    public SimpleName(TokenRange tokenRange, String identifier) {
        super(tokenRange);
        setIdentifier(identifier);
        customInitialization();
    }

    @Override
    @Generated("com.github.braisdom.jds.generator.core.node.AcceptGenerator")
    public <R, A> R accept(final GenericVisitor<R, A> v, final A arg) {
        return v.visit(this, arg);
    }

    @Override
    @Generated("com.github.braisdom.jds.generator.core.node.AcceptGenerator")
    public <A> void accept(final VoidVisitor<A> v, final A arg) {
        v.visit(this, arg);
    }

    @Generated("com.github.braisdom.jds.generator.core.node.PropertyGenerator")
    public String getIdentifier() {
        return identifier;
    }

    @Generated("com.github.braisdom.jds.generator.core.node.PropertyGenerator")
    public SimpleName setIdentifier(final String identifier) {
        assertNonEmpty(identifier);
        if (identifier == this.identifier) {
            return (SimpleName) this;
        }
        notifyPropertyChange(ObservableProperty.IDENTIFIER, this.identifier, identifier);
        this.identifier = identifier;
        return this;
    }

    @Override
    @Generated("com.github.braisdom.jds.generator.core.node.RemoveMethodGenerator")
    public boolean remove(Node node) {
        if (node == null)
            return false;
        return super.remove(node);
    }

    public String asString() {
        return identifier;
    }

    @Override
    @Generated("com.github.braisdom.jds.generator.core.node.CloneGenerator")
    public SimpleName clone() {
        return (SimpleName) accept(new CloneVisitor(), null);
    }

    @Override
    @Generated("com.github.braisdom.jds.generator.core.node.GetMetaModelGenerator")
    public SimpleNameMetaModel getMetaModel() {
        return JavaParserMetaModel.simpleNameMetaModel;
    }

    @Override
    @Generated("com.github.braisdom.jds.generator.core.node.ReplaceMethodGenerator")
    public boolean replace(Node node, Node replacementNode) {
        if (node == null)
            return false;
        return super.replace(node, replacementNode);
    }
}