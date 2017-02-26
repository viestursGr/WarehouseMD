/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package WarehouseSessionBean;

import WarehouseEntities.Inventory;
import java.math.BigDecimal;
import java.util.List;
import javax.ejb.Local;

/**
 *
 * @author Viesturs
 */
@Local
public interface WarehouseSessionBeanLocal {
    public List<Inventory> getByPriceRange(BigDecimal min, BigDecimal max);
    public List<Inventory> getByStockReserve();
}
