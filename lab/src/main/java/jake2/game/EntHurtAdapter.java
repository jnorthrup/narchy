package jake2.game;

public abstract class EntHurtAdapter extends SuperAdapter
{
    public abstract void hurt(edict_t self, edict_t other, int damage);
}
