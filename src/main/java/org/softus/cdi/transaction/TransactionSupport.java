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
package org.softus.cdi.transaction;

import java.util.Map;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

/**
 * <p>
 * Provides the ability to manage transaction boundaries, register
 * synchronization objects, and associate resource objects with the current
 * transaction.
 * </p>
 *
 * @author Vlad Arkhipov
 */
public interface TransactionSupport
{
    /**
     * Creates a new transaction and associate it with the current thread.
     *
     * @throws NotSupportedException thrown if the thread is already associated
     *         with a transaction and the Transaction Manager implementation
     *         does not support nested transactions.
     * @throws SystemException thrown if the transaction manager encounters an
     *         unexpected error condition.
     */
    public void begin()
    throws NotSupportedException, SystemException;

    /**
     * Complete the transaction associated with the current thread. When this
     * method completes, the thread is no longer associated with a transaction.
     *
     * @throws RollbackException thrown to indicate that the transaction has
     *         been rolled back rather than committed.
     * @throws HeuristicMixedException thrown to indicate that a heuristic
     *         decision was made and that some relevant updates have been
     *         committed while others have been rolled back.
     * @throws HeuristicRollbackException thrown to indicate that a heuristic
     *         decision was made and that all relevant updates have been rolled
     *         back.
     * @throws SecurityException thrown to indicate that the thread is not
     *         allowed to commit the transaction.
     * @throws IllegalStateException thrown if the current thread is not
     *         associated with a transaction.
     * @throws SystemException thrown if the transaction manager encounters an
     *         unexpected error condition.
     */
    public void commit()
    throws RollbackException, HeuristicMixedException,
        HeuristicRollbackException, SecurityException, IllegalStateException,
        SystemException;

    /**
     * Roll back the transaction associated with the current thread. When this
     * method completes, the thread is no longer associated with a transaction.
     *
     * @throws IllegalStateException thrown if the current thread is not
     *         associated with a transaction.
     * @throws SecurityException thrown to indicate that the thread is not
     *         allowed to roll back the transaction.
     * @throws SystemException thrown if the transaction manager encounters an
     *         unexpected error condition.
     */
    public void rollback()
    throws IllegalStateException, SecurityException, SystemException;

    /**
     * Obtain the status of the transaction associated with the current thread.
     *
     * @return the transaction status. If no transaction is associated with the
     *         current thread, this method returns the
     *         {@link Status#STATUS_NO_TRANSACTION} value.
     * @throws SystemException thrown if the transaction manager encounters an
     *         unexpected error condition.
     */
    public int getStatus()
    throws SystemException;

    /**
     * Suspend the transaction currently associated with the calling thread and
     * return a Transaction object that represents the transaction context being
     * suspended. If the calling thread is not associated with a transaction,
     * the method returns a null object reference. When this method returns, the
     * calling thread is not associated with a transaction.
     *
     * @return transaction object representing the suspended transaction.
     * @throws SystemException thrown if the transaction manager encounters an
     *         unexpected error condition.
     */
    public Transaction suspend()
    throws SystemException;

    /**
     * Resume the transaction context association of the calling thread with the
     * transaction represented by the supplied Transaction object. When this
     * method returns, the calling thread is associated with the transaction
     * context specified.
     *
     * @param transaction the {@link Transaction} object that represents the
     *        transaction to be resumed.
     * @throws InvalidTransactionException thrown if the parameter transaction
     *         object contains an invalid transaction.
     * @throws IllegalStateException thrown if the thread is already associated
     *         with another transaction.
     * @throws SystemException thrown if the transaction manager encounters an
     *         unexpected error condition.
     */
    public void resume(
        Transaction transaction)
    throws InvalidTransactionException, IllegalStateException,
        SystemException;

    /**
     * Modify the transaction associated with the current thread such that the
     * only possible outcome of the transaction is to roll back the transaction.
     *
     * @throws IllegalStateException thrown if the current thread is not
     *         associated with a transaction.
     * @throws SystemException thrown if the transaction manager encounters an
     *         unexpected error condition.
     */
    public void setRollbackOnly()
    throws IllegalStateException, SystemException;

    /**
     * Register a synchronization object for the transaction currently
     * associated with the current thread. The transaction manager invokes the
     * beforeCompletion method prior to starting the two-phase transaction
     * commit process. After the transaction is completed, the transaction
     * manager invokes the afterCompletion method.
     *
     * @param synchronization the {@link Synchronization} object for the
     *        transaction associated with the target object.
     * @throws RollbackException thrown to indicate that the transaction has
     *         been marked for rollback only.
     * @throws SystemException thrown if the transaction manager encounters an
     *         unexpected error condition.
     * @throws IllegalStateException thrown if the transaction in the target
     *         object is in the prepared state or the transaction is inactive.
     */
    public void registerSynchronization(
        Synchronization synchronization)
    throws RollbackException, SystemException, IllegalStateException;

    /**
     * Get an object from the {@link Map} of resources being managed for the
     * transaction bound to the current thread at the time this method is
     * called. The key should have been supplied earlier by a call to putResouce
     * in the same transaction. If the key cannot be found in the current
     * resource Map, null is returned. The general contract of this method is
     * that of {@link Map#get(Object)} for a {@link Map} that supports non-null
     * keys and null values. For example, the returned value is null if there is
     * no entry for the parameter key or if the value associated with the key is
     * actually null.
     *
     * @param key the key for the {@link Map} entry.
     * @return the value associated with the key.
     */
    public Object getResource(
        Object key);

    /**
     * Add or replace an object in the {@link Map} of resources being managed
     * for the transaction bound to the current thread at the time this method
     * is called. The supplied key should be of an caller-defined class so as
     * not to conflict with other users. The class of the key must guarantee
     * that the hashCode and equals methods are suitable for use as keys in a
     * map. The key and value are not examined or used by the implementation.
     * The general contract of this method is that of
     * {@link Map#put(Object, Object)} for a {@link Map} that supports non-null
     * keys and null values. For example, if there is already an value
     * associated with the key, it is replaced by the value parameter.
     *
     * @param key the key for the {@link Map} entry.
     * @param value the value for the {@link Map} entry.
     */
    public void putResource(
        Object key,
        Object value);
}
