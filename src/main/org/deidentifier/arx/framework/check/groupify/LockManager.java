/*
 * ARX: Powerful Data Anonymization
 * Copyright 2012 - 2015 Florian Kohlmayer, Fabian Prasser
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.deidentifier.arx.framework.check.groupify;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import org.deidentifier.arx.framework.check.groupify.LockManager.SpinLock.Lock;


public class LockManager {
    
    /**
     * try(SpinLock.Lock lock = spinlock.lock())
     * {
     *   // something very quick and non blocking
     * }
     */
    public static class SpinLock
    {
        private final AtomicReference<Thread> _lock   = new AtomicReference<>(null);
        private final Lock                    _unlock = new Lock();

        public Lock lock()
        {
            Thread thread = Thread.currentThread();
            while (true)
            {
                if (!_lock.compareAndSet(null, thread))
                {
                    if (_lock.get() == thread) throw new IllegalStateException("SpinLock is not reentrant");
                    continue;
                }
                return _unlock;
            }
        }

        public class Lock implements AutoCloseable
        {
            @Override
            public void close()
            {
                _lock.set(null);
            }
        }
    }
    
    private SpinLock[] locksBucket;
    private SpinLock   lockRehash = new SpinLock();
    private SpinLock   lockCreate = new SpinLock();
    
    public LockManager(int buckets) {
        this.locksBucket = new SpinLock[buckets];
        for (int i=0; i<this.locksBucket.length; i++) {
            this.locksBucket[i] = new SpinLock();
        }
    }
    
    public void setNumberOfBuckets(int size) {

        if (size == this.locksBucket.length) {
            return;
        } else if (size < this.locksBucket.length) {
            this.locksBucket = Arrays.copyOf(this.locksBucket, size);
        } else {
            this.locksBucket = new SpinLock[size];
            for (int i=0; i<this.locksBucket.length; i++) {
                this.locksBucket[i] = new SpinLock();
            }
        }
    }
    
    public Lock lockBucket(int bucket) {
        return locksBucket[bucket].lock();
    }

    public Lock lockCreate() {
        return lockCreate.lock();
    }
    
    public Lock lockRehash() {
        return lockRehash.lock();
    }
}