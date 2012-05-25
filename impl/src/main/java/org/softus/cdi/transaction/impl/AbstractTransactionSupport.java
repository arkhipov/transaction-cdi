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

import javax.annotation.PostConstruct;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.softus.cdi.transaction.TransactionSupport;

/**
 * <p>
 * Transaction support that uses {@link TransactionManager} and
 * {@link TransactionSynchronizationRegistry} to implement
 * {@link TransactionSupport} interface.
 * </p>
 *
 * @author Vlad Arkhipov
 */
public abstract class AbstractTransactionSupport
implements TransactionSupport
{
    private TransactionManager transactionManager;

    private TransactionSynchronizationRegistry synchronizationRegistry;

    /**
     * Creates a new transaction support.
     */
    public AbstractTransactionSupport()
    {

    }

    /**
     * Looks up a transaction manager.
     *
     * @return {@link TransactionManager}.
     */
    protected abstract TransactionManager lookupTransactionManager();

    /**
     * Looks up a transaction synchronization registry.
     *
     * @return {@link TransactionSynchronizationRegistry}.
     */
    protected abstract TransactionSynchronizationRegistry lookupSynchronizationRegistry();

    /**
     * Initializes the current object.
     */
    @PostConstruct
    void setup()
    {
        transactionManager = lookupTransactionManager();
        synchronizationRegistry = lookupSynchronizationRegistry();
    }

    /**
     * {@inheritDoc}
     */
    public void begin()
    throws NotSupportedException, SystemException
    {
        transactionManager.begin();
    }

    /**
     * {@inheritDoc}
     */
    public void commit()
    throws RollbackException, HeuristicMixedException,
        HeuristicRollbackException, SecurityException, IllegalStateException,
        SystemException
    {
        transactionManager.commit();
    }

    /**
     * {@inheritDoc}
     */
    public void rollback()
    throws IllegalStateException, SecurityException, SystemException
    {
        transactionManager.rollback();
    }

    /**
     * {@inheritDoc}
     */
    public int getStatus()
    throws SystemException
    {
        return transactionManager.getStatus();
    }

    /**
     * {@inheritDoc}
     */
    public Transaction suspend()
    throws SystemException
    {
        return transactionManager.suspend();
    }

    /**
     * {@inheritDoc}
     */
    public void resume(
        Transaction transaction)
    throws InvalidTransactionException, IllegalStateException, SystemException
    {
        transactionManager.resume(transaction);
    }

    /**
     * {@inheritDoc}
     */
    public void setRollbackOnly()
    throws IllegalStateException, SystemException
    {
        transactionManager.setRollbackOnly();
    }

    /**
     * {@inheritDoc}
     */
    public void registerSynchronization(
        Synchronization synchronization)
    {
        synchronizationRegistry
            .registerInterposedSynchronization(synchronization);
    }

    /**
     * {@inheritDoc}
     */
    public Object getResource(
        Object key)
    {
        return synchronizationRegistry.getResource(key);
    }

    /**
     * {@inheritDoc}
     */
    public void putResource(
        Object key,
        Object value)
    {
        synchronizationRegistry.putResource(key, value);
    }
}
