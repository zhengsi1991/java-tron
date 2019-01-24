package org.tron.core.db.datacheck;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.tron.core.config.args.Args;


public class NeedDataCheckCondition implements Condition {

  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    return Args.getInstance().getDataCheckPoint() > 0;
  }
}