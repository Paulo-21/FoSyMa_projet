package eu.su.mas.dedaleEtu.mas.knowledge;
import java.io.Serializable;

public class WrapperBlockage implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1481294841444505020L;
	public int nb_blockage;
	public boolean try_solve_block;
	public WrapperBlockage(int nb , boolean tryb) {
		this.nb_blockage = nb;
		this.try_solve_block = tryb;
	}
	public int nb_blockage() {
		return nb_blockage;
	}
	public void set_nb_blockage(int nb_blockage) {
		this.nb_blockage = nb_blockage;
	}
	public void add_nb_blockage(int nb_blockage) {
		this.nb_blockage += nb_blockage;
	}
	public void minus_nb_blockage(int nb_blockage) {
		this.nb_blockage -= nb_blockage;
	}
	public boolean try_solve_block() {
		return try_solve_block;
	}
	public void set_try_solve_block(boolean try_solve_blockage) {
		this.try_solve_block = try_solve_blockage;
	}
}
