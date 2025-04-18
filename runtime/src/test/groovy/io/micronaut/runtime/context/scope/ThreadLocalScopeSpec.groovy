/*
 * Copyright 2017-2019 original authors
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
package io.micronaut.runtime.context.scope

import io.micronaut.context.ApplicationContext
import io.micronaut.context.LifeCycle
import io.micronaut.context.annotation.Factory
import io.micronaut.core.annotation.AnnotationUtil
import io.micronaut.inject.BeanDefinition
import io.micronaut.support.AbstractBeanDefinitionSpec
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.util.concurrent.PollingConditions

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.Target

import static java.lang.annotation.RetentionPolicy.RUNTIME

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class ThreadLocalScopeSpec extends AbstractBeanDefinitionSpec {

    void "test parse bean definition data"() {
        when:
        BeanDefinition beanDefinition = buildBeanDefinition('test.ThreadLocalBean', '''
package test;

import io.micronaut.runtime.context.scope.*;

@ThreadLocal
class ThreadLocalBean {

}
''')

        then:
        beanDefinition.getAnnotationNameByStereotype(AnnotationUtil.SCOPE).get() == ThreadLocal.name

    }

    void "test bean definition data"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run("test")
        BeanDefinition aDefinition = applicationContext.getBeanDefinition(A)

        expect:
        aDefinition.getAnnotationNameByStereotype(AnnotationUtil.SCOPE).isPresent()
        aDefinition.getAnnotationNameByStereotype(AnnotationUtil.SCOPE).get() == ThreadLocal.name

    }

    void "test thread local scope on class"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run("test")
        B b = applicationContext.getBean(B)

        when:
        b.a.num = 2

        boolean isolated = false
        Thread.start {
            isolated = b.a.total() == 0
            b.a.setNum(4)
            assert b.a.total() == 4
        }.join()


        then:
        b.a.total() == 2
        isolated

    }

    void "test thread local scope on class with meta annotation"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run("test")
        ScopeOnMetaAnnotationB b = applicationContext.getBean(ScopeOnMetaAnnotationB)

        when:
        b.a.num = 2

        boolean isolated = false
        Thread.start {
            isolated = b.a.total() == 0
            b.a.setNum(4)
            assert b.a.total() == 4
        }.join()


        then:
        b.a.total() == 2
        isolated

    }

    void "test thread local scope on interface"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run("test")
        BAndInterface b = applicationContext.getBean(BAndInterface)

        when:
        b.a.num = 2

        boolean isolated = false
        Thread.start {
            isolated = b.a.total() == 0
            b.a.setNum(4)
            assert b.a.total() == 4
        }.join()


        then:
        b.a.total() == 2
        isolated

    }


    void "test thread local scope on factory"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run("test")
        BAndFactory b = applicationContext.getBean(BAndFactory)

        when:
        b.a.num = 2

        boolean isolated = false
        Thread.start {
            isolated = b.a.total() == 0
            b.a.setNum(4)
            assert b.a.total() == 4
        }.join()


        then:
        b.a.total() == 2
        isolated
    }

    void "cleaned up on context close"() {
        given:
        def ctx = ApplicationContext.run("test")
        def listener = ctx.getBean(CleanupListener)

        def t1 = new Thread(() -> {
            ctx.getBean(LifecycleBean).someMethod()
        })
        t1.start()
        t1.join()

        def t2 = new Thread(() -> {
            ctx.getBean(LifecycleBean).someMethod()
        })
        t2.start()
        t2.join()

        when:
        ctx.close()
        then:
        listener.numCleaned == 2
    }

    void "cleaned up on thread termination"() {
        given:
        def ctx = ApplicationContext.run("test")
        def listener = ctx.getBean(CleanupListener)

        when:
        def t1 = new Thread(() -> {
            ctx.getBean(LifecycleBean).someMethod()
        })
        t1.start()
        t1.join()

        def t2 = new Thread(() -> {
            ctx.getBean(LifecycleBean).someMethod()
        })
        t2.start()
        t2.join()

        then:
        new PollingConditions().eventually {
            triggerGc()
            listener.numCleaned == 2
        }

        cleanup:
        ctx.close()
    }

    private static volatile long sink

    private static void triggerGc() {
        System.gc()
        for (int i = 0; i < 1000; i++) {
            sink = System.identityHashCode(new byte[10000]);
        }
    }
}

@ThreadLocal
class A {
    int num

    int total() {
        return num
    }
}

@Singleton
class B {
    private A a

    B(A a) {
        this.a = a
    }

    A getA() {
        return a
    }
}

@MyAnn
class ScopeOnMetaAnnotationA {
    int num

    int total() {
        return num
    }
}

@Singleton
class ScopeOnMetaAnnotationB {
    private ScopeOnMetaAnnotationA a

    ScopeOnMetaAnnotationB(ScopeOnMetaAnnotationA a) {
        this.a = a
    }

    ScopeOnMetaAnnotationA getA() {
        return a
    }
}

interface IA {
    void setNum(int num)

    int total()
}

@ThreadLocal
class AImpl implements IA {
    int num

    @Override
    int total() {
        return num
    }
}

@Singleton
class BAndInterface {
    private IA a

    BAndInterface(IA a) {
        this.a = a
    }

    IA getA() {
        return a
    }
}

interface IA2 {
    void setNum(int num)

    int total()
}

class A2Impl implements IA2 {
    int num

    @Override
    int total() {
        return num
    }
}

@Factory
class  IA2Factory {

    @ThreadLocal
    IA2 a() {
        return new A2Impl()
    }
}

@Singleton
class BAndFactory {
    private IA2 a

    BAndFactory(IA2 a) {
        this.a = a
    }

    IA2 getA() {
        return a
    }
}

@ThreadLocal
@Retention(RUNTIME)
@Target([ ElementType.TYPE, ElementType.METHOD])
public @interface MyAnn {
}

@Singleton
class CleanupListener {
    int numCleaned = 0
}

@ThreadLocal(lifecycle = true)
class LifecycleBean implements LifeCycle<LifecycleBean> {
    @Inject
    CleanupListener cleanupListener

    void someMethod() {}

    @Override
    boolean isRunning() {
        return true
    }

    @Override
    LifecycleBean stop() {
        cleanupListener.numCleaned++
        return this
    }
}
