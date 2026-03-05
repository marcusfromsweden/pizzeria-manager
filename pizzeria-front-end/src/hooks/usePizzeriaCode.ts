import { usePizzeriaContext } from '../routes/PizzeriaProvider';

export const usePizzeriaCode = () => {
  const { pizzeriaCode } = usePizzeriaContext();
  return pizzeriaCode;
};

export default usePizzeriaCode;
