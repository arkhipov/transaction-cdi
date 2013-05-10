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

import javax.ejb.ApplicationException;
import javax.ejb.TransactionAttributeType;

import javax.inject.Inject;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.softus.cdi.transaction.TransactionSupport;
import org.softus.cdi.transaction.Transactional;

/**
 * <p>
 * Implements container-managed transaction control.
 * </p>
 *
 * @author Vlad Arkhipov
 */
@Transactional
@Interceptor
class TransactionalInterceptor
{
    private static final Logger log = LoggerFactory
        .getLogger(TransactionalInterceptor.class);

    @Inject
    private TransactionExtension extension;

    @Inject
    private TransactionSupport transactionSupport;

    /**
     * Creates a new interceptor.
     */
    TransactionalInterceptor()
    {

    }

    @AroundInvoke
    Object aroundInvoke(
        InvocationContext invocationContext)
    throws Exception
    {
        boolean active = isTransactionActive();
        TransactionAttributeType attr = extension
            .getTransactionAttribute(invocationContext.getMethod());
        Boolean requiresNew = requiresNew(active, attr);

        boolean debug = log.isDebugEnabled();

        if (debug)
        {
            log.debug("Invoking transactional method "
                + invocationContext.getMethod() + ", attr = " + attr
                + ", active = " + active + ", requiresNew = " + requiresNew);
        }

        // Suspend the current transaction if transaction attribute is
        // REQUIRES_NEW or NOT_SUPPORTED.
        Transaction previous = null;
        if ((requiresNew != Boolean.FALSE) && active)
        {
            if (debug)
            {
                log.debug("Suspending the current transaction");
            }

            previous = transactionSupport.suspend();
        }

        try
        {
            if (requiresNew == Boolean.TRUE)
            {
                if (debug)
                {
                    log.debug("Starting a new transaction");
                }

                transactionSupport.begin();
            }

            Object result;
            try
            {
                result = invocationContext.proceed();
            }
            catch (Exception e)
            {
                if (requiresNew == Boolean.FALSE)
                {
                    if (needsRollback(e))
                    {
                        transactionSupport.setRollbackOnly();
                    }
                }
                else if (requiresNew == Boolean.TRUE)
                {
                    if (needsRollback(e))
                    {
                        if (debug)
                        {
                            log.debug("Rolling back the current transaction");
                        }

                        transactionSupport.rollback();
                    }
                    else
                    {
                        if (debug)
                        {
                            log.debug("Committing the current transaction");
                        }

                        transactionSupport.commit();
                    }
                }

                throw e;
            }

            if (requiresNew == Boolean.TRUE)
            {
                if (transactionSupport.getStatus() == Status.STATUS_MARKED_ROLLBACK)
                {
                    if (debug)
                    {
                        log.debug("Rolling back the current transaction");
                    }

                    transactionSupport.rollback();
                }
                else
                {
                    if (debug)
                    {
                        log.debug("Committing the current transaction");
                    }

                    transactionSupport.commit();
                }
            }

            return result;
        }
        finally
        {
            // Resume the previous transaction if it was suspended.
            if (previous != null)
            {
                if (debug)
                {
                    log.debug("Resuming the previous transaction");
                }

                transactionSupport.resume(previous);
            }
        }
    }

    /**
     * Checks if the current transaction is active, rolled back or marked for
     * rollback.
     *
     * @return {@code true} if the current transaction is active, rolled back or
     *         marked for rollback, {@code false} otherwise.
     * @throws SystemException thrown if the transaction manager encounters an
     *         unexpected error condition
     */
    private boolean isTransactionActive()
    throws SystemException
    {
        switch (transactionSupport.getStatus())
        {
        case Status.STATUS_ACTIVE:
        case Status.STATUS_MARKED_ROLLBACK:
        case Status.STATUS_ROLLEDBACK:
            return true;

        default:
            return false;
        }
    }

    /**
     * Determines whether it is necessary to begin a new transaction.
     *
     * @param active the status of the current transaction.
     * @param attribute the transaction attribute of the current method.
     * @return {@code Boolean.TRUE} if the interceptor should suspend the
     *         current transaction and invoke the method within a new
     *         transaction, {@code Boolean.FALSE} if the interceptor should
     *         invoke the method within the current transaction, {@code null} if
     *         the interceptor should suspend the current transaction and invoke
     *         the method outside of transaction.
     */
    private Boolean requiresNew(
        boolean active,
        TransactionAttributeType attribute)
    {
        switch (attribute)
        {
        case MANDATORY:
            if (active)
            {
                return false;
            }
            else
            {
                throw new IllegalStateException(
                    "Transaction is required to perform this method");
            }

        case NEVER:
            if (!active)
            {
                return null;
            }
            else
            {
                throw new IllegalStateException(
                    "This method cannot be invoked within a transaction");
            }

        case NOT_SUPPORTED:
            return null;

        case REQUIRED:
            return !active;

        case REQUIRES_NEW:
            return true;

        case SUPPORTS:
            if (active)
            {
                return false;
            }
            else
            {
                return null;
            }

        default:
            throw new UnsupportedOperationException(
                "Unsupported TransactionAttribute value " + attribute);
        }
    }

    /**
     * Determines whether it is necessary to rollback the current transaction
     * when the specified exception occurred during the method invocation.
     *
     * @param exception the exception that occurred during the method
     *        invocation.
     * @return {@code true} if the interceptor should rollback the current
     *         transaction, {@code false} if the interceptor should commit the
     *         current transaction.
     */
    private boolean needsRollback(
        Exception exception)
    {
        boolean rollback = exception instanceof RuntimeException;

        for (Class<?> clazz = exception.getClass(); clazz != null; clazz = clazz
            .getSuperclass())
        {
            ApplicationException ae = clazz
                .getAnnotation(ApplicationException.class);
            if (ae != null)
            {
                if (ae.inherited())
                {
                    return ae.rollback();
                }
                else
                {
                    break;
                }
            }
        }

        return rollback;
    }
}
