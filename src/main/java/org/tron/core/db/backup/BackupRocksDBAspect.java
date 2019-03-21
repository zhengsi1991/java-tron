package org.tron.core.db.backup;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.tron.core.capsule.BlockCapsule;

@Slf4j
@Aspect
public class BackupRocksDBAspect {

  @Autowired
  private BackupDbUtil util;

  private boolean[] isBaking = {false};

  @Pointcut("execution(** org.tron.core.db.Manager.pushBlock(..)) && args(block)")
  public void pointPushBlock(BlockCapsule block) {

  }

  @Before("pointPushBlock(block)")
  public void backupDb(BlockCapsule block) {
    try {
      if (!isBaking[0]) {
        isBaking[0] = true;
        new Thread(new Runnable() {
          @Override
          public void run() {
            util.doBackup(block, isBaking);
          }
        }).start();
      } else {
        logger.info("there are one baking thread working currently.");
      }
    } catch (Exception e) {
      isBaking[0] = false;
      logger.error("backup db failure: {}", e);
    }
  }

  @AfterThrowing("pointPushBlock(block)")
  public void logErrorPushBlock(BlockCapsule block) {
    logger.info("AfterThrowing pushBlock");
  }
}