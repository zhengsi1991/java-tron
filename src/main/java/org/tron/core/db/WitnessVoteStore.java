package org.tron.core.db;


import java.util.Spliterator;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class WitnessVoteStore extends TronDatabase<byte[]> {

  @Autowired
  public WitnessVoteStore(ApplicationContext ctx) {
    super("witnessvote");
  }

  @Override
  public void put(byte[] key, byte[] item) {
    this.getDbSource().putData(key, item);
  }

  @Override
  public void delete(byte[] key) {

  }

  @Override
  public byte[] get(byte[] key) {
    return this.getDbSource().getData(key);
  }

  @Override
  public boolean has(byte[] key) {
    return false;
  }

  @Override
  public void forEach(Consumer action) {

  }

  @Override
  public Spliterator spliterator() {
    return null;
  }
}