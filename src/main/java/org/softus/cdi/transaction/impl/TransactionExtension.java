/*
 *    Copyright 2012 The Softus Team.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.softus.cdi.transaction.impl;

import java.lang.reflect.Method;

import java.util.HashMap;
import java.util.Map;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import javax.enterprise.event.Observes;

import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessManagedBean;
import javax.enterprise.inject.spi.ProcessSessionBean;

import javax.enterprise.util.AnnotationLiteral;

import org.softus.cdi.transaction.Transactional;

/**
 * <p>
 * Extension that allows to use standard EJB annotations for transaction
 * management in CDI managed beans.
 * </p>
 *
 * @author Vlad Arkhipov
 */
class TransactionExtension
implements Extension
{
    private Map<Method, TransactionAttributeType> transactionAttributes;

    /**
     * Creates a new extension.
     */
    TransactionExtension()
    {
        transactionAttributes = new HashMap<Method, TransactionAttributeType>();
    }

    /**
     * Observes {@link ProcessManagedBean} event.
     *
     * @param event {@link ProcessManagedBean} event.
     */
    <X> void processBean(
        @Observes ProcessManagedBean<X> event)
    {
        boolean sessionBean = event instanceof ProcessSessionBean<?>;
        AnnotatedType<X> annotatedType = event.getAnnotatedBeanClass();
        boolean hasClassInterceptor = annotatedType
            .isAnnotationPresent(Transactional.class);
        if (hasClassInterceptor && sessionBean)
        {
            event.addDefinitionError(new RuntimeException(
                "@Transactional is forbidden for session bean "
                    + event.getBean()));
        }
        else
        {
            TransactionAttribute classAttr = annotatedType
                .getAnnotation(TransactionAttribute.class);
            for (AnnotatedMethod<? super X> am : annotatedType.getMethods())
            {
                boolean hasMethodInterceptor = am
                    .isAnnotationPresent(Transactional.class);
                if (hasMethodInterceptor && sessionBean)
                {
                    event.addDefinitionError(new RuntimeException(
                        "@Transactional is forbidden for session bean method "
                        + am));
                }
                else if (hasClassInterceptor || hasMethodInterceptor)
                {
                    TransactionAttribute attr = am
                        .getAnnotation(TransactionAttribute.class);
                    Method method = am.getJavaMember();
                    TransactionAttributeType attrType =
                        mergeTransactionAttributes(classAttr, attr);
                    transactionAttributes.put(method, attrType);
                }
            }
        }
    }

    /**
     * Observes {@link AfterBeanDiscovery} event.
     *
     * @param event {@link AfterBeanDiscovery} event.
     * @param beanManager {@link BeanManager}.
     */
    void afterBeanDiscovered(
        @Observes AfterBeanDiscovery event,
        BeanManager beanManager)
    {
        event.addContext(new TransactionalContext(beanManager));
    }

    /**
     * Calculates a transaction attribute for the specified method. If there is
     * no attribute declared directly on the method, the class attribute is
     * used.
     *
     * @param classAttribute {@link TransactionAttribute} of the class.
     * @param methodAttribute {@link TransactionAttribute} of the method.
     * @return transaction attribute of the method.
     */
    private TransactionAttributeType mergeTransactionAttributes(
        TransactionAttribute classAttribute,
        TransactionAttribute methodAttribute)
    {
        if (methodAttribute != null)
        {
            return methodAttribute.value();
        }

        if (classAttribute != null)
        {
            return classAttribute.value();
        }

        return TransactionAttributeType.REQUIRED;
    }

    /**
     * Retrieves a transaction attribute for the specified method.
     *
     * @param method the method.
     * @return a transaction attribute or {@code null} if the method is not
     *         transactional.
     */
    TransactionAttributeType getTransactionAttribute(
        Method method)
    {
        return transactionAttributes.get(method);
    }

    /**
     * Annotation literal for {@link Transactional}.
     *
     * @author Vlad Arkhipov
     */
    @SuppressWarnings("all")
    private static class TransactionalLiteral
    extends AnnotationLiteral<Transactional>
    implements Transactional
    {
        private static final long serialVersionUID = -5204983598749555200L;

        static final TransactionalLiteral INSTANCE = new TransactionalLiteral();

        private TransactionalLiteral()
        {
        }
    }
}
