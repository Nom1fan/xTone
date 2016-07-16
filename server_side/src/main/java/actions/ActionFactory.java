package actions;

import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import Exceptions.NoSuchClientActionException;
import MessagesToServer.ServerActionType;
import annotations.ServerActionAnno;
import log.Logged;

/**
 * Created by Mor on 23/04/2016.
 */
public class ActionFactory extends Logged {

    private Map<ServerActionType, Class<? extends ServerAction>> actionType2Action = new HashMap<>();
    private static ActionFactory _instance;

    private ActionFactory() {
        super();
        Reflections reflections = new Reflections();
        Set<Class<? extends ServerAction>> classes = reflections.getSubTypesOf(ServerAction.class);
        for (Class<? extends ServerAction> aClass : classes) {
            if(!Modifier.isAbstract(aClass.getModifiers())) {
                if(aClass.isAnnotationPresent(ServerActionAnno.class)) {
                    ServerActionAnno caAnno = aClass.getAnnotation(ServerActionAnno.class);
                    ServerActionType caType = caAnno.actionType();
                    actionType2Action.put(caType, aClass);
                }
            }
        }

    }

    public static ActionFactory instance() {
        if(_instance == null)
            _instance = new ActionFactory();
        return _instance;
    }

    public ServerAction getAction(ServerActionType serverActionType) throws NoSuchClientActionException {
        Class<? extends ServerAction> aClass = actionType2Action.get(serverActionType);
        try {
            return aClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            throw new NoSuchClientActionException(e.getMessage());
        }
    }
}
