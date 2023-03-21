(ns patience.db.protocol)

(defprotocol IPatients
  (amount [this filters])
  (list-by [this opts])
  (get-by-id [this patient-id])
  (create! [this patient])
  (delete! [this patient-id])
  (update! [this patient-id new-state]))
