(ns patience.db.protocol)

(defprotocol IPatients
  (list-all [this])
  (list-filtered [this filters])
  (get-by-id [this patient-id])
  (create! [this patient])
  (delete! [this patient-id])
  (update! [this patient-id new-state]))
