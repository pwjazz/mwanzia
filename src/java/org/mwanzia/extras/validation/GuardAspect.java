package org.mwanzia.extras.validation;

import net.sf.oval.guard.GuardAspect2;

import org.aspectj.lang.annotation.Aspect;

@Aspect
public class GuardAspect extends GuardAspect2 {
    public GuardAspect() {
        super();
        setGuard(new MwanziaGuard());
    }
}
