/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.groovy.scripts.internal;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;
import org.gradle.api.internal.provider.DefaultProperty;

import javax.annotation.Nullable;
import java.net.URI;
import java.nio.file.Paths;

public class AssignmentProvenanceTransformer extends AbstractScriptTransformer {
    private final URI location;

    public AssignmentProvenanceTransformer(@Nullable URI location) {
        this.location = location;
    }

    @Override
    protected int getPhase() {
        return Phases.CANONICALIZATION;
    }

    @Override
    public void call(SourceUnit source) throws CompilationFailedException {
        AstUtils.visitScriptCode(source, new Visitor());
    }

    class Visitor extends CodeVisitorSupport {

        /**
         * a = b
         * =>
         * clearProv(a = withProv("...", b))
         *
         * a = b = c
         * =>
         * clearProv(a = withProv("...", clearProv(b = withProv("...", c))))
         */
        @Override
        public void visitBinaryExpression(BinaryExpression expr) {
            if (expr.getClass() == BinaryExpression.class && expr.getOperation().getText().equals("=")) {
                Expression rhs = expr.getRightExpression();
                expr.setRightExpression(
                    new StaticMethodCallExpression(
                        ClassHelper.make(DefaultProperty.class),
                        "withProv",
                        new ArgumentListExpression(new Expression[] {
                            rhs,
                            getSourceLocation(),
                            new ConstantExpression(rhs.getLineNumber()),
                            new ConstantExpression(rhs.getColumnNumber())
                        })
                    )
                );
            }
            super.visitBinaryExpression(expr);
        }
    }

    private ConstantExpression getSourceLocation() {
        // FIXME-RC: we only collect the name (and even that is unreliable), as it may come from a cached .class file compiled from a different file name
        return location == null ? ConstantExpression.NULL : new ConstantExpression(Paths.get(location.getPath()).getFileName().toString());
    }
}
