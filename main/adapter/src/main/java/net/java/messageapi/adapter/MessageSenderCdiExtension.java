package net.java.messageapi.adapter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.*;

import net.java.messageapi.MessageApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class MessageSenderCdiExtension implements Extension {
    private final Logger log = LoggerFactory.getLogger(MessageSenderCdiExtension.class);

    // TODO provide the set of discovered message apis for injection

    private final Set<Class<?>> messageApis = Sets.newHashSet();
    private final Set<BeanId> beanIds = Sets.newHashSet();

    <X> void step1_discoverMessageApis(@Observes ProcessAnnotatedType<X> pat) {
        discoverMessageApi(pat);
        vetoMessageApiImplementations(pat);
    }

    private <X> void discoverMessageApi(ProcessAnnotatedType<X> pat) {
        AnnotatedType<X> annotatedType = pat.getAnnotatedType();

        MessageApi annotation = annotatedType.getAnnotation(MessageApi.class);
        if (annotation != null) {
            Class<X> messageApi = annotatedType.getJavaClass();
            log.info("discovered message api {}", messageApi.getName());
            messageApis.add(messageApi);
        }
    }

    private <X> void vetoMessageApiImplementations(ProcessAnnotatedType<X> pat) {
        // TODO what happens when the impl is scanned before the api?
        AnnotatedType<X> annotatedType = pat.getAnnotatedType();
        final Set<Type> typeClosure = annotatedType.getTypeClosure();
        Set<Type> intersection = Sets.intersection(typeClosure, messageApis);
        if (!annotatedType.getJavaClass().isInterface() && !intersection.isEmpty()) {
            pat.veto();
            log.info(
                    "Preventing {} from being installed as bean, as it's a receiver for message api {}",
                    annotatedType.getJavaClass(), intersection);
        }
    }

    <X> void step2_discoverInjectionTargets(@Observes ProcessInjectionTarget<X> pit) {
        for (InjectionPoint injectionPoint : pit.getInjectionTarget().getInjectionPoints()) {
            log.debug("scan injection point {}", injectionPoint);
            Class<?> type = getMessageApi(injectionPoint);
            if (messageApis.contains(type)) {
                final Class<X> api = getMessageApi(injectionPoint);
                final Set<Annotation> qualifiers = injectionPoint.getQualifiers();
                log.info(
                        "discovered injection point named \"{}\" in {} for message api {} qualified as {}",
                        new Object[] { injectionPoint.getMember().getName(),
                                getBeanName(injectionPoint), api.getSimpleName(), qualifiers });
                boolean added = beanIds.add(new BeanId(api, qualifiers));
                if (!added) {
                    log.info("bean already defined");
                }
            }
        }
    }

    private <X> Class<X> getMessageApi(InjectionPoint injectionPoint) {
        if (!(injectionPoint.getType() instanceof Class))
            return null;
        @SuppressWarnings("unchecked")
        final Class<X> type = (Class<X>) injectionPoint.getType();
        if (Instance.class.equals(type)) {
            @SuppressWarnings("unchecked")
            Class<Instance<?>> instance = (Class<Instance<?>>) type;
            // FIXME resolve Instance type
            log.info("generic interfaces: {}", (Object) instance.getGenericInterfaces());
        }
        return type;
    }

    private Object getBeanName(InjectionPoint injectionPoint) {
        final Bean<?> bean = injectionPoint.getBean();
        return (bean == null) ? "???" : bean.getBeanClass().getSimpleName();
    }

    void step3_createBeans(@Observes AfterBeanDiscovery abd, BeanManager bm) {
        log.info("create {} beans for {} message apis", beanIds.size(), messageApis.size());
        for (BeanId beanId : beanIds) {
            log.info("create bean for {}", beanId);
            MessageApiBean<?> bean = MessageApiBean.of(beanId.type, beanId.qualifiers);
            abd.addBean(bean);
        }
    }
}
